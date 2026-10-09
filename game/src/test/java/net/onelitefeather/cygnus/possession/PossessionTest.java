package net.onelitefeather.cygnus.possession;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PossessionTest extends CygnusPlayerTestBase {

    @Test
    @DisplayName("A possession is over once its seconds have passed")
    void overAfterItsSeconds(Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = env.createPlayer(instance, new Pos(0, 40, 0));

        Possession possession = Possession.start(slender, 1_000L, 8);

        assertFalse(possession.isOver(8_999L));
        assertTrue(possession.isOver(9_000L));
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("Only survivors in the creek's instance and within the range count")
    void rangeAroundTheCreek(Env env) {
        Instance instance = env.createFlatInstance();
        Instance other = env.createFlatInstance();
        EntityCreature creek = new EntityCreature(EntityType.CREAKING);
        creek.setInstance(instance, new Pos(0, 40, 0)).join();
        Player near = env.createPlayer(instance, new Pos(10, 40, 0));
        Player edge = env.createPlayer(instance, new Pos(0, 40, 20));
        Player far = env.createPlayer(instance, new Pos(21, 40, 0));
        Player elsewhere = env.createPlayer(other, new Pos(1, 40, 0));

        Set<Player> inRange = Possession.inRange(creek, 20.0D, List.of(near, edge, far, elsewhere));

        assertEquals(Set.of(near, edge), inRange);
        env.destroyInstance(instance, true);
        env.destroyInstance(other, true);
    }
}
