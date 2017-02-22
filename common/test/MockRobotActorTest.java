import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;
import whs.common.net.*;
import whs.util.ChannelPair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by misson20000 on 2/12/17.
 */
public class MockRobotActorTest {
    private void pumpUntil(CompletableFuture future, Connection... connections) {
        while(!future.isDone()) {
            for(Connection c : connections) {
                c.pumpInput();
            }
        }
    }

    @Test
    public void fullActorModelTest() throws IOException, ExecutionException, InterruptedException {
        String driverName = "Test Driver";
        String subsystemName = "Test Subsystem";

        LocalActorSet<PeerDriver> robotActors = new LocalActorSet<>();
        LocalRobot localRobot = new LocalRobot();
        LocalSubsystem localSubsystem = new LocalSubsystem(subsystemName);
        robotActors.addActor(localRobot);
        robotActors.addActor(localSubsystem);
        localRobot.addSubsystem(localSubsystem);

        LocalActorSet<PeerRobot> driverActors = new LocalActorSet<>();
        LocalDriver localDriver = new LocalDriver(driverName);
        driverActors.addActor(localDriver);

        ChannelPair channels = new ChannelPair();
        channels.getChannelA().configureBlocking(false);
        channels.getChannelB().configureBlocking(false);
        Connection<PeerDriver> robot = new Connection<>(channels.getChannelA(), robotActors, "robotest", "1.0");
        Connection<PeerRobot> driver = new Connection<>(channels.getChannelB(), driverActors, "robotest", "1.0");

        RemoteDriver remoteDriver = new RemoteDriver(robot.getRemoteRootReference());
        RemoteRobot remoteRobot = new RemoteRobot(driver.getRemoteRootReference());

        CompletableFuture<String> driverNameFuture = remoteDriver.getName();
        pumpUntil(driverNameFuture, robot, driver);
        assertEquals(driverNameFuture.get(), driverName);

        CompletableFuture<List<RemoteSubsystem>> subsystemsFuture = remoteRobot.getSubsystems();
        pumpUntil(subsystemsFuture, robot, driver);
        List<RemoteSubsystem> remoteSubsystems = subsystemsFuture.get();
        assertEquals(remoteSubsystems.size(), 1);

        CompletableFuture<String> subsystemNameFuture = remoteSubsystems.get(0).getName();
        pumpUntil(subsystemNameFuture, robot, driver);
        assertEquals(subsystemNameFuture.get(), subsystemName);
    }

    public static class PeerRobot {
        
    }
    public static class PeerDriver {
        
    }
    public static class LocalRobot extends AbstractLocalActor<PeerDriver> {
        private List<LocalSubsystem> subsystems = new ArrayList<>();

        public void addSubsystem(LocalSubsystem subsystem) {
            subsystems.add(subsystem);
        }

        @RequestHandler
        public List<LocalSubsystem> getSubsystems() {
            return subsystems;
        }
    }
    public static class LocalDriver extends AbstractLocalActor<PeerRobot> {
        private final String name;

        public LocalDriver(String name) {
            this.name = name;
        }

        @RequestHandler
        public String getName() {
            return name;
        }
    }
    public static class LocalSubsystem extends AbstractLocalActor<PeerDriver> {
        private final String name;

        public LocalSubsystem(String name) {
            this.name = name;
        }

        @RequestHandler
        public String getName() {
            return name;
        }
    }
    public static class RemoteRobot extends RemoteActor {
        public RemoteRobot(RemoteActorReference actorReference) {
            super(actorReference);
        }

        public CompletableFuture<String> getName() {
            return this.request("getName", null, String.class);
        }

        public CompletableFuture<List<RemoteSubsystem>> getSubsystems() {
            return this.<List<RemoteActorReference>>request("getSubsystems", null, new TypeToken<List<RemoteActorReference>>() {}.getType())
                    .thenApply((response) -> response.stream().map(RemoteSubsystem::new).collect(Collectors.toList()));
        }
    }
    public static class RemoteDriver extends RemoteActor {
        public RemoteDriver(RemoteActorReference actorReference) {
            super(actorReference);
        }

        public CompletableFuture<String> getName() {
            return this.request("getName", null, String.class);
        }
    }
    public static class RemoteSubsystem extends RemoteActor {
        public RemoteSubsystem(RemoteActorReference actorReference) {
            super(actorReference);
        }

        public CompletableFuture<String> getName() {
            return this.request("getName", null, String.class);
        }
    }
}