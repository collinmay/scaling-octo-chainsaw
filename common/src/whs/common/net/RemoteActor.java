package whs.common.net;

import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;

/**
 * Created by misson20000 on 2/12/17.
 */
public abstract class RemoteActor implements SidedActor {
    final RemoteActorReference actorReference;

    public RemoteActor(RemoteActorReference actorReference) {
        this.actorReference = actorReference;
    }

    //protected <A extends Request<B>, B> CompletableFuture<B> request(A request, Type type) {
    //    return actorReference.connection.fireRequest(actorReference.id, request, type);
    //}

    protected <T> CompletableFuture<T> request(String remoteFunction, Object parameter, Type returnType) {
       return actorReference.connection.fireRequest(actorReference.id, remoteFunction, parameter, returnType);
    }
}
