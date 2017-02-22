package whs.common.net;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.util.concurrent.CompletionStage;

/**
 * Created by misson20000 on 2/12/17.
 */
public interface LocalActor<A> extends SidedActor {
    default void onConnectionClosed(A connection, Exception cause) {
    }

    void setReference(LocalActorReference reference);
    LocalActorReference getReference();

    CompletionStage<JsonElement> dispatchRequest(String function, Object parameter, Gson gson);
}
