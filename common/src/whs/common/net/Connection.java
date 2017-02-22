package whs.common.net;

import com.google.gson.*;
import whs.util.Pair;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Created by misson20000 on 2/12/17.
 */
public class Connection<Attachment> {
    private LocalActorSet<Attachment> localActors;
    private HashMap<Integer, Pair<CompletableFuture, Type>> outstandingRequests = new HashMap<>();
    private short requestId;
    private ByteChannel channel;
    private Attachment attachment;
    private Gson gson;
    private JsonParser jsonParser = new JsonParser();
    private boolean forwardExceptions;

    private HandshakeState handshakeState;

    // packet reader
    private ByteBuffer headerInputBuffer = ByteBuffer.allocate(4);
    private ByteBuffer bodyInputBuffer = ByteBuffer.allocate(65536);
    private int packetSize = -1;

    // packet writer
    private ByteBuffer bodyOutputBuffer = ByteBuffer.allocate(65536);
    private Handshake localHandshake;

    public Connection(ByteChannel channel,
                      LocalActorSet<Attachment> localActors,
                      String protocolName, String protocolVersion,
                      boolean forwardExceptions) throws IOException {
        this.channel = channel;
        this.localActors = localActors;
        this.forwardExceptions = forwardExceptions;

        headerInputBuffer.clear();
        bodyInputBuffer.clear();

        {
            GsonBuilder gson = new GsonBuilder();
            ActorReferenceAdapter actorReferenceAdapter = new ActorReferenceAdapter(this);
            gson.registerTypeAdapter(RemoteActorReference.class, actorReferenceAdapter);
            gson.registerTypeAdapter(LocalActorReference.class, actorReferenceAdapter);
            gson.registerTypeHierarchyAdapter(LocalActor.class, new ActorAdapter(this));
            this.gson = gson.create();
        }

        bodyOutputBuffer.clear();
        String handshakeText = gson.toJson(localHandshake = new Handshake(protocolName, protocolVersion));
        bodyOutputBuffer.putInt(handshakeText.length());
        bodyOutputBuffer.put(handshakeText.getBytes());
        bodyOutputBuffer.flip();
        channel.write(bodyOutputBuffer);
        this.handshakeState = HandshakeState.SENT_HANDSHAKE;
    }

    public Connection(ByteChannel channel,
                      LocalActorSet<Attachment> localActors,
                      String protocolName, String protocolVersion) throws IOException {
        this(channel, localActors, protocolName, protocolVersion, true);
    }

    public Attachment getAttachment() {
        return attachment;
    }
    public SelectorTrigger createSelectorTrigger() {
        return this::pumpInput;
    }

    public boolean pumpInput() {
        try {
            if(packetSize < 0) {
                while(headerInputBuffer.remaining() > 0) {
                    switch(channel.read(headerInputBuffer)) {
                        case 0:
                            return true;
                        case -1:
                            connectionClosed(null);
                            return false;
                        default:
                    }
                }
                headerInputBuffer.flip();
                packetSize = headerInputBuffer.getInt();
                headerInputBuffer.compact();
            }
            bodyInputBuffer.limit(packetSize);
            while(bodyInputBuffer.remaining() > 0) {
                switch(channel.read(bodyInputBuffer)) {
                    case 0:
                        return true;
                    case -1:
                        connectionClosed(null);
                        return false;
                    default:
                }
            }
            bodyInputBuffer.flip();

            byte[] bytes = new byte[packetSize];
            bodyInputBuffer.get(bytes);
            packetSize = -1;
            bodyInputBuffer.clear();
            JsonObject packet = jsonParser.parse(new String(bytes)).getAsJsonObject();
            if(!packet.get("message").isJsonPrimitive()) {
                throw new ProtocolErrorException("message type is either not present or not a json primitive");
            }
            switch(packet.get("message").getAsString()) {
                case "request":
                    LocalActor<Attachment> targetActor = localActors.getActorById(packet.getAsJsonPrimitive("targetActor").getAsInt());
                    final JsonObject jsonResponse = new JsonObject();
                    jsonResponse.addProperty("message", "response");
                    jsonResponse.addProperty("requestId", packet.get("requestId").getAsInt());
                    targetActor.dispatchRequest(packet.get("requestFunction").getAsString(), packet.get("request"), gson).whenComplete((responseElement, exception) -> {
                        if(exception == null) {
                            jsonResponse.add("response", responseElement);
                            jsonResponse.addProperty("status", "successful");
                        } else {
                            exception.printStackTrace();
                            jsonResponse.addProperty("status", "exceptional");
                            if(forwardExceptions) {
                                jsonResponse.addProperty("exceptionMessage", exception.getMessage());
                            }
                        }
                        String jsonText = gson.toJson(jsonResponse);

                        bodyOutputBuffer.clear();
                        bodyOutputBuffer.putInt(jsonText.length());
                        bodyOutputBuffer.put(jsonText.getBytes());
                        bodyOutputBuffer.flip();
                        try {
                            channel.write(bodyOutputBuffer);
                        } catch(IOException e) {
                            connectionClosed(e);
                        }
                    });
                    break;
                case "response":
                    Pair<CompletableFuture, Type> request = outstandingRequests.get(packet.get("requestId").getAsInt());
                    switch(packet.get("status").getAsString()) {
                        case "successful":
                            request.a.complete(gson.fromJson(packet.get("response"), request.b));
                            outstandingRequests.remove(packet.get("requestId").getAsInt());
                            break;
                        case "exceptional":
                            request.a.completeExceptionally(new RemoteException(packet.get("exceptionMessage").getAsString()));
                            break;
                    }
                    break;
                case "handshake":
                    Handshake foreignHandshake = gson.fromJson(packet, Handshake.class);
                    if(!foreignHandshake.equals(localHandshake)) {
                        connectionClosed(new ProtocolMismatchException(foreignHandshake));
                        channel.close();
                    } else {
                        this.handshakeState = HandshakeState.GOT_FOREIGN_HANDSHAKE;
                    }
                    break;
                default:
                    connectionClosed(new ProtocolErrorException("unknown message type '" + packet.get("message").getAsString() + "'"));
                    channel.close();
            }
        } catch(ProtocolErrorException e){
            try {
                channel.close();
            } catch(IOException e1) {
            }
            connectionClosed(e);
            return false;
        } catch(IOException e){
            connectionClosed(e);
            return false;
        }
        return true;
    }

    // null means end of steam
    private void connectionClosed(Exception cause) {
        localActors.stream().forEach((actor) -> actor.onConnectionClosed(attachment, cause));
        outstandingRequests.forEach((id, request) -> request.a.completeExceptionally(cause));
    }

    public <A> CompletableFuture<A> fireRequest(int id, String function, Object request, Type type) {
        requestId++;
        CompletableFuture future = new CompletableFuture();
        outstandingRequests.put((int) requestId, new Pair<>(future, type));

        JsonObject json = new JsonObject();
        json.addProperty("message", "request");
        json.addProperty("targetActor", id);
        json.addProperty("requestId", requestId);
        json.addProperty("requestFunction", function);
        json.add("request", gson.toJsonTree(request));

        String jsonText = gson.toJson(json);

        bodyOutputBuffer.clear();
        bodyOutputBuffer.putInt(jsonText.length());
        bodyOutputBuffer.put(jsonText.getBytes());
        bodyOutputBuffer.flip();
        try {
            channel.write(bodyOutputBuffer);
        } catch(IOException e) {
            connectionClosed(e);
        }

        return future;
    }

    public RemoteActorReference getRemoteRootReference() {
        return new RemoteActorReference(this, 0);
    }

    public void setAttachment(Attachment attachment) {
        this.attachment = attachment;
    }

    private enum HandshakeState {
        SENT_HANDSHAKE,
        GOT_FOREIGN_HANDSHAKE
    }
}
