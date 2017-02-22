package whs.common.net;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Created by misson20000 on 2/13/17.
 */
public abstract class AbstractLocalActor<T> implements LocalActor<T> {
    private LocalActorReference reference;
    private HashMap<String, Method> handlers = new HashMap<>();

    public AbstractLocalActor() {
        for(Method m : this.getClass().getMethods()) {
            if(m.isAnnotationPresent(RequestHandler.class)) {
                if(m.getParameterCount() <= 1) {
                    handlers.put(m.getName(), m);
                }
            }
        }
    }

    @Override
    public void onConnectionClosed(T connection, Exception cause) {
    }

    @Override
    public void setReference(LocalActorReference reference) {
        this.reference = reference;
    }

    @Override
    public LocalActorReference getReference() {
        return this.reference;
    }

    @Override
    public CompletionStage<JsonElement> dispatchRequest(String function, Object request, Gson gson) {
        try {
            if(handlers.containsKey(function)) {
                Method m = handlers.get(function);
                Object ret = m.getParameterCount() == 0 ? m.invoke(this) : m.invoke(this, request);
                CompletionStage<Object> stage;
                if(CompletionStage.class.isAssignableFrom(m.getReturnType())) {
                    stage = (CompletionStage<Object>) ret;
                } else {
                    stage = CompletableFuture.completedFuture(ret);
                }
                return stage.thenApply(gson::toJsonTree);
            } else {
                throw new DispatchException(function);
            }
        } catch(IllegalAccessException | InvocationTargetException | DispatchException e) {
            CompletableFuture<JsonElement> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
}
