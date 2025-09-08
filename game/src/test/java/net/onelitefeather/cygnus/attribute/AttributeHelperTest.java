package net.onelitefeather.cygnus.attribute;

import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MicrotusExtension.class)
class AttributeHelperTest {

    @Test
    void testAttributeAdjustment(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        AttributeHelper.adjustStepHeightAndJump(player);
        assertEquals(0.0, player.getAttribute(Attribute.JUMP_STRENGTH).getBaseValue());
        assertEquals(1.0, player.getAttribute(Attribute.STEP_HEIGHT).getBaseValue());
        env.destroyInstance(instance, true);
    }

    @Test
    void testResetAttributeAdjustments(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        AttributeHelper.adjustStepHeightAndJump(player);
        assertEquals(0.0, player.getAttribute(Attribute.JUMP_STRENGTH).getBaseValue());
        assertEquals(1.0, player.getAttribute(Attribute. STEP_HEIGHT).getBaseValue());

        AttributeHelper.resetAttributeAdjustments(player);
        assertEquals(0.42, player.getAttribute(Attribute.JUMP_STRENGTH).getBaseValue());
        assertEquals(0.6, player.getAttribute(Attribute. STEP_HEIGHT).getBaseValue());
        env.destroyInstance(instance, true);
    }

    @Test
    void testHealthScaleUpdate(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        assertEquals(20.0, player.getAttribute(Attribute.MAX_HEALTH).getBaseValue());

        float healthOnTop = 20.0f;
        AttributeHelper.updateHealthScale(player, healthOnTop);

        assertEquals(40.0, player.getAttribute(Attribute.MAX_HEALTH).getBaseValue());
        env.destroyInstance(instance, true);
    }
}
