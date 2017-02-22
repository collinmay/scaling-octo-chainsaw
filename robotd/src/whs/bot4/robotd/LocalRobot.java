package whs.bot4.robotd;

import whs.common.net.AbstractLocalActor;
import whs.common.net.RequestHandler;

/**
 * Created by misson20000 on 2/13/17.
 */
public class LocalRobot extends AbstractLocalActor<RemoteDriver> {
    @RequestHandler
    public String getName() {
        return "robotd";
    }
}
