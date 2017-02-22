package whs.common.net;

import com.google.gson.*;

import java.lang.reflect.Type;

/**
 * Created by misson20000 on 2/13/17.
 */
public class ActorAdapter implements JsonSerializer<LocalActor> {
    private Connection connection;

    public ActorAdapter(Connection connection) {
        this.connection = connection;
    }

    @Override
    public JsonElement serialize(LocalActor src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.getReference().getId());
    }
}
