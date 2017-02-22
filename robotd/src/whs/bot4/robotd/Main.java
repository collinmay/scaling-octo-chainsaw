package whs.bot4.robotd;

import whs.common.net.Connection;
import whs.common.net.LocalActorSet;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;

/**
 * Created by misson20000 on 2/13/17.
 */
public class Main {
    public static void main(String[] args) throws IOException {
        int port = 7574;

        System.out.println("robotd: 1.0");

        LocalActorSet<RemoteDriver> actorSet = new LocalActorSet<>();

        Selector socketSelector = SelectorProvider.provider().openSelector();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        InetSocketAddress address = new InetSocketAddress(port);
        serverChannel.socket().bind(address);

        serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT).attach((Runnable) () -> {
            try {
                SocketChannel socket = serverChannel.accept();
                socket.configureBlocking(false);
                Connection<RemoteDriver> connection = new Connection<>(socket, actorSet, "whs robotocol", "1.0");
                socket.register(socketSelector, SelectionKey.OP_READ).attach(connection.createSelectorTrigger());
            } catch(IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        });

        System.out.println("robotd listening on " + port);
        while(true) {
            socketSelector.select();
            Iterator<SelectionKey> keys = socketSelector.selectedKeys().iterator();
            while(keys.hasNext()) {
                SelectionKey key = keys.next();
                keys.remove();

                if(!key.isValid()) {
                    continue;
                }

                ((Runnable) key.attachment()).run();
            }
        }
    }
}
