package whs.common.net;

/**
 * Created by misson20000 on 2/13/17.
 */
public class RemoteActorReference {
    final Connection<?> connection;
    final int id;

    public RemoteActorReference(Connection<?> c, int id) {
        this.connection = c;
        this.id = id;
    }
}
