package whs.common.net;

/**
 * Created by misson20000 on 2/12/17.
 */
public interface SelectorTrigger {
    // returns whether or not to remove the channel from the selection set
    //  true: remove
    //  false :keep
    public boolean trigger();
}
