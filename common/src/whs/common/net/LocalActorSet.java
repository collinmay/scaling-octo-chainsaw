package whs.common.net;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by misson20000 on 2/13/17.
 */
public class LocalActorSet<T> {
    private List<LocalActor<T>> actors;

    public LocalActorSet() {
        actors = new ArrayList<>();
    }

    public void addActor(LocalActor<T> actor) {
        int index = actors.size();
        actors.add(actor);
        actor.setReference(new LocalActorReference(index));
    }

    public LocalActor<T> getActorById(int id) {
        return actors.get(id);
    }

    public Stream<LocalActor<T>> stream() {
        return actors.stream();
    }
}
