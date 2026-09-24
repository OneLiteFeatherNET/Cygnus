package net.onelitefeather.cygnus.creek.body;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.monster.CreakingMeta;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreakingBodyIntegrationTest extends CygnusPlayerTestBase {

    @Test
    @DisplayName("He is shown to the chosen viewers and nobody else")
    void showsHimOnlyToTheChosen(Env env) {
        Instance instance = env.createFlatInstance();
        Player first = env.createConnection().connect(instance, new Pos(0, 40, 0));
        Player second = env.createConnection().connect(instance, new Pos(2, 40, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 10));

        assertTrue(body.entity().getViewers().isEmpty(), "nobody sees him before he is shown");

        body.showTo(Set.of(first.getUuid()));

        assertTrue(body.entity().getViewers().contains(first));
        assertFalse(body.entity().getViewers().contains(second));
        assertTrue(body.isVisibleTo(first.getUuid()));

        body.showTo(Set.of());

        assertTrue(body.entity().getViewers().isEmpty());
    }

    @Test
    @DisplayName("The hunting look lights his eyes and freezing stops him")
    void looksFollowTheState(Env env) {
        Instance instance = env.createFlatInstance();
        env.createConnection().connect(instance, new Pos(0, 40, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 10));
        CreakingMeta meta = (CreakingMeta) body.entity().getEntityMeta();

        body.setAggressive(true);
        assertTrue(meta.isActive());

        body.setFrozen(true);
        assertFalse(meta.canMove());

        body.setFrozen(false);
        assertTrue(meta.canMove());
    }

    @Test
    @DisplayName("Removing takes him out of the world")
    void removeTakesHimOut(Env env) {
        Instance instance = env.createFlatInstance();
        env.createConnection().connect(instance, new Pos(0, 40, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 10));

        body.remove();

        assertTrue(body.entity().isRemoved());
    }

    @Test
    @DisplayName("A goal the navigator turned down does not block the next one")
    void rejectedGoalDoesNotBlockTheNext(Env env) {
        Instance instance = env.createFlatInstance();
        env.createConnection().connect(instance, new Pos(0, 40, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0.5, 40, 10.5));

        // Same block as he stands in: the navigator has nothing to do and turns it down.
        body.moveTo(new Pos(0.7, 40, 10.7), 0.25D);
        Pos next = new Pos(1.4, 40, 10.9);
        body.moveTo(next, 0.25D);

        assertEquals(next, ((EntityCreature) body.entity()).getNavigator().getGoalPosition());
    }
}
