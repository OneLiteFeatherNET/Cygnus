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

        assertEquals(0.42, player.getAttribute(Attribute.JUMP_STRENGTH).getBaseValue(), 0.0001);
        assertEquals(0.0, player.getAttribute(Attribute.JUMP_STRENGTH).getValue(), 0.0001);

        assertEquals(0.6, player.getAttribute(Attribute.STEP_HEIGHT).getBaseValue(), 0.0001);
        assertEquals(1.0, player.getAttribute(Attribute.STEP_HEIGHT).getValue(), 0.0001);
        env.destroyInstance(instance, true);
    }

    @Test
    void testAttributeAdjustmentIsIdempotent(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        AttributeHelper.adjustStepHeightAndJump(player);
        AttributeHelper.adjustStepHeightAndJump(player);

        assertEquals(0.0, player.getAttribute(Attribute.JUMP_STRENGTH).getValue(), 0.0001);
        assertEquals(1.0, player.getAttribute(Attribute.STEP_HEIGHT).getValue(), 0.0001);
        env.destroyInstance(instance, true);
    }

    @Test
    void testResetAttributeAdjustments(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        AttributeHelper.adjustStepHeightAndJump(player);
        assertEquals(0.0, player.getAttribute(Attribute.JUMP_STRENGTH).getValue(), 0.0001);
        assertEquals(1.0, player.getAttribute(Attribute.STEP_HEIGHT).getValue(), 0.0001);

        AttributeHelper.resetAttributeAdjustments(player);
        assertEquals(0.42, player.getAttribute(Attribute.JUMP_STRENGTH).getValue(), 0.0001);
        assertEquals(0.6, player.getAttribute(Attribute.STEP_HEIGHT).getValue(), 0.0001);
        env.destroyInstance(instance, true);
    }

    @Test
    void testHealthScaleUpdate(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        assertEquals(20.0, player.getAttribute(Attribute.MAX_HEALTH).getBaseValue());

        float healthOnTop = 20.0f;
        AttributeHelper.updateHealthScale(player, healthOnTop);

        assertEquals(20.0, player.getAttribute(Attribute.MAX_HEALTH).getBaseValue(), 0.0001);
        assertEquals(40.0, player.getAttribute(Attribute.MAX_HEALTH).getValue(), 0.0001);
        assertEquals(40.0f, player.getHealth(), 0.0001f);

        AttributeHelper.removeHealthScale(player);
        assertEquals(20.0, player.getAttribute(Attribute.MAX_HEALTH).getValue(), 0.0001);
        env.destroyInstance(instance, true);
    }

    @Test
    void testHealthScaleUpdateIsIdempotent(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        AttributeHelper.updateHealthScale(player, 20.0f);
        AttributeHelper.updateHealthScale(player, 20.0f);

        assertEquals(40.0, player.getAttribute(Attribute.MAX_HEALTH).getValue(), 0.0001);
        env.destroyInstance(instance, true);
    }

    @Test
    void testDecreaseAndResetSpeed(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        assertEquals(0.1, player.getAttribute(Attribute.MOVEMENT_SPEED).getValue(), 0.0001);

        AttributeHelper.decreaseSpeed(player);
        assertEquals(0.1, player.getAttribute(Attribute.MOVEMENT_SPEED).getBaseValue(), 0.0001);
        assertEquals(0.065, player.getAttribute(Attribute.MOVEMENT_SPEED).getValue(), 0.0001);

        AttributeHelper.resetSpeed(player);
        assertEquals(0.1, player.getAttribute(Attribute.MOVEMENT_SPEED).getValue(), 0.0001);
        env.destroyInstance(instance, true);
    }

    @Test
    void testSpeedScaleUpdate(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        AttributeHelper.decreaseSpeed(player);

        AttributeHelper.updateSpeedScale(player, 0.01);

        assertEquals(0.1, player.getAttribute(Attribute.MOVEMENT_SPEED).getBaseValue(), 0.0001);
        assertEquals(0.075, player.getAttribute(Attribute.MOVEMENT_SPEED).getValue(), 0.0001);

        AttributeHelper.removeSpeedScale(player);
        assertEquals(0.065, player.getAttribute(Attribute.MOVEMENT_SPEED).getValue(), 0.0001);
        env.destroyInstance(instance, true);
    }

    @Test
    void testSpeedScaleUpdateIsIdempotent(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        AttributeHelper.decreaseSpeed(player);

        AttributeHelper.updateSpeedScale(player, 0.01);
        AttributeHelper.updateSpeedScale(player, 0.01);

        assertEquals(0.075, player.getAttribute(Attribute.MOVEMENT_SPEED).getValue(), 0.0001);
        env.destroyInstance(instance, true);
    }

    @Test
    void testSlenderDrainingSpeedModifier(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.1);

        AttributeHelper.applySlenderDrainingSpeed(player);
        assertEquals(0.0669, player.getAttribute(Attribute.MOVEMENT_SPEED).getValue(), 0.001);

        AttributeHelper.removeSlenderDrainingSpeed(player);
        assertEquals(0.1, player.getAttribute(Attribute.MOVEMENT_SPEED).getValue(), 0.001);
        env.destroyInstance(instance, true);
    }

    @Test
    void testResetAllRemovesEveryModifierTheRoundApplied(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        AttributeHelper.adjustStepHeightAndJump(player);
        AttributeHelper.decreaseSpeed(player);
        AttributeHelper.updateSpeedScale(player, 0.02);
        AttributeHelper.updateHealthScale(player, 6.0f);
        AttributeHelper.applySlenderDrainingSpeed(player);

        AttributeHelper.resetAll(player);

        assertEquals(0.42, player.getAttribute(Attribute.JUMP_STRENGTH).getValue(), 0.0001);
        assertEquals(0.6, player.getAttribute(Attribute.STEP_HEIGHT).getValue(), 0.0001);
        assertEquals(0.1, player.getAttribute(Attribute.MOVEMENT_SPEED).getValue(), 0.0001);
        assertEquals(20.0, player.getAttribute(Attribute.MAX_HEALTH).getValue(), 0.0001);
        env.destroyInstance(instance, true);
    }

    @Test
    void testResetAllClampsTheHealthToTheLoweredMaximum(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        AttributeHelper.updateHealthScale(player, 6.0f);
        assertEquals(26.0f, player.getHealth(), 0.0001f);

        AttributeHelper.resetAll(player);

        assertEquals(20.0f, player.getHealth(), 0.0001f, "the bonus health must not survive the removal of the modifier");
        env.destroyInstance(instance, true);
    }
}
