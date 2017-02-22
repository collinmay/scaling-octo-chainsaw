package whs.common.net;

import com.google.gson.*;

import java.lang.reflect.Type;

/**
 * Created by misson20000 on 2/13/17.
 */
public class ActorReferenceAdapter implements JsonSerializer<LocalActorReference>, JsonDeserializer<RemoteActorReference> {
    private Connection connection;

    public ActorReferenceAdapter(Connection connection) {
        this.connection = connection;
    }

    @Override
    public RemoteActorReference deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            return new RemoteActorReference(connection, json.getAsInt());
        } catch(Exception e) {
            throw new JsonParseException(e);
        }
    }

    @Override
    public JsonElement serialize(LocalActorReference src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.getId());
    }
}
