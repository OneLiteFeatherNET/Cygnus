package net.onelitefeather.cygnus.creek.consequence;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatchLaunchIntegrationTest extends CygnusPlayerTestBase {

    @Test
    @DisplayName("A catch throws the survivor upwards with the speed of the configured height")
    void throwsUpwards(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        CatchLaunch launch = new CatchLaunch(_ -> {
        }, 5.0D);

        launch.apply(survivor);

        assertEquals(CatchLaunch.launchSpeed(5.0D), survivor.getVelocity().y(), 1.0E-9D);
        assertEquals(0.0D, survivor.getVelocity().x(), 0.0D, "the throw goes straight up");
        assertEquals(0.0D, survivor.getVelocity().z(), 0.0D, "the throw goes straight up");
    }

    @Test
    @DisplayName("The rest of the punishment is applied before the launch")
    void appliesTheOtherConsequenceFirst(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        List<Double> velocityWhenWrapped = new ArrayList<>();
        CatchLaunch launch = new CatchLaunch(player -> velocityWhenWrapped.add(player.getVelocity().y()), 5.0D);

        launch.apply(survivor);

        assertEquals(List.of(0.0D), velocityWhenWrapped, "the wrapped consequence ran before the throw");
        assertTrue(survivor.getVelocity().y() > 0.0D);
    }

    @Test
    @DisplayName("A height of 0 leaves the survivor where they are but still punishes them")
    void zeroHeightDoesNotLaunch(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        List<Player> punished = new ArrayList<>();
        CatchLaunch launch = new CatchLaunch(punished::add, 0.0D);

        launch.apply(survivor);

        assertEquals(0.0D, survivor.getVelocity().y(), 0.0D);
        assertEquals(List.of(survivor), punished);
    }

    @Test
    @DisplayName("Cleaning up is passed on to the wrapped consequence")
    void cleanUpIsPassedOn(Env env) {
        boolean[] cleaned = {false};
        CatchLaunch launch = new CatchLaunch(new CatchConsequence() {
            @Override
            public void apply(Player survivor) {
            }

            @Override
            public void cleanUp() {
                cleaned[0] = true;
            }
        }, 5.0D);

        launch.cleanUp();

        assertTrue(cleaned[0]);
    }
}
