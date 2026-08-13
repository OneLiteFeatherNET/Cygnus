package net.onelitefeather.cygnus.player;

import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CygnusPlayerTest extends CygnusPlayerTestBase {

    @Test
    void testSetSprintingRemovesSprintModifierWhenBlocked(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer player = (CygnusPlayer) env.createPlayer(instance);

        player.setSprinting(true);
        assertTrue(player.getAttribute(Attribute.MOVEMENT_SPEED).modifiers().contains(CygnusPlayer.SPEED_MODIFIER_SPRINTING));
        assertFalse(player.getAttribute(Attribute.MOVEMENT_SPEED).modifiers().contains(CygnusPlayer.DISABLED_SPRINT_MODIFIER));

        player.setBlockedSprinting(true);
        player.setSprinting(true);
        assertFalse(player.getAttribute(Attribute.MOVEMENT_SPEED).modifiers().contains(CygnusPlayer.SPEED_MODIFIER_SPRINTING));
        assertTrue(player.getAttribute(Attribute.MOVEMENT_SPEED).modifiers().contains(CygnusPlayer.DISABLED_SPRINT_MODIFIER));

        env.destroyInstance(instance, true);
    }
}
