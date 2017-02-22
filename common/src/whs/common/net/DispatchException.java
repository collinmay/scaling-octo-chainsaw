package whs.common.net;

/**
 * Created by misson20000 on 2/13/17.
 */
public class DispatchException extends Exception {
    public DispatchException(Exception e) {
        super(e);
    }

    public DispatchException(String function) {
        super("could not find function: " + function);
    }
}
