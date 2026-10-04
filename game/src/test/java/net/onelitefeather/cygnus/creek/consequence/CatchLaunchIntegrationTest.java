package net.onelitefeather.cygnus.creek.consequence;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatchLaunchIntegrationTest extends CygnusPlayerTestBase {

    @Test
    @DisplayName("A catch throws the survivor upwards with the speed of the configured height")
    void throwsUpwards(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        CatchLaunch launch = new CatchLaunch(5.0D);

        launch.perform(survivor);

        assertEquals(CatchLaunch.launchSpeed(5.0D), survivor.getVelocity().y(), 1.0E-9D);
        assertEquals(0.0D, survivor.getVelocity().x(), 0.0D, "the throw goes straight up");
        assertEquals(0.0D, survivor.getVelocity().z(), 0.0D, "the throw goes straight up");
    }

    @Test
    @DisplayName("A height of 0 leaves the survivor where they are")
    void zeroHeightDoesNotLaunch(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        CatchLaunch launch = new CatchLaunch(0.0D);

        assertTrue(launch.perform(survivor), "a switched-off launch is not a failure");

        assertEquals(0.0D, survivor.getVelocity().y(), 0.0D);
    }
}
