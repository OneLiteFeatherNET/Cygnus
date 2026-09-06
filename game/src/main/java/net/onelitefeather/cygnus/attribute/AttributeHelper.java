package net.onelitefeather.cygnus.attribute;

import net.kyori.adventure.key.Key;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeInstance;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;

/**
 * The {@link AttributeHelper} class provides utility methods to adjust the player's attributes.
 *
 * @author theEvilReaper
 * @version 1.1.0
 * @since 1.0.0
 */
@SuppressWarnings("java:S3252")
public final class AttributeHelper {

    public static final Key SLENDER_DRAINING_SPEED_KEY = Key.key("cygnus", "slender_draining");
    public static final Key SPEED_SCALING_KEY = Key.key("cygnus", "speed_scaling");
    public static final Key HEALTH_SCALING_KEY = Key.key("cygnus", "health_scaling");

    private static final AttributeModifier SLENDER_DRAINING_SPEED_MODIFIER = new AttributeModifier(
                    SLENDER_DRAINING_SPEED_KEY,
            -0.331,
            AttributeOperation.ADD_MULTIPLIED_TOTAL
    );

    private static final double DEFAULT_JUMP_STRENGTH = 0.42;
    private static final double GAME_JUMP_STRENGTH = 0.0;

    private static final double DEFAULT_STEP_HEIGHT = 0.6;
    private static final double GAME_STEP_HEIGHT = 1.0;

    private static final double DEFAULT_MOVE_SPEED = 0.1;
    private static final double GAME_MOVE_SPEED = 0.065;


    /**
     * Adjusts the step height and jump strength for the player.
     * The game values are used to prevent the player from jumping but increase the step height.
     *
     * @param player the player to adjust
     */
    public static void adjustStepHeightAndJump(Player player) {
        player.getAttribute(Attribute.JUMP_STRENGTH).setBaseValue(GAME_JUMP_STRENGTH);
        player.getAttribute(Attribute.STEP_HEIGHT).setBaseValue(GAME_STEP_HEIGHT);
    }

    /**
     * Resets the step height and jump strength for the player.
     * The default values are used to reset the player's attributes.
     *
     * @param player the player to reset
     */
    public static void resetAttributeAdjustments(Player player) {
        player.getAttribute(Attribute.JUMP_STRENGTH).setBaseValue(DEFAULT_JUMP_STRENGTH);
        player.getAttribute(Attribute.STEP_HEIGHT).setBaseValue(DEFAULT_STEP_HEIGHT);
    }

    /**
     * Increases the player's speed to the game value.
     *
     * @param player the player to increase the speed
     */
    public static void decreaseSpeed(Player player) {
        player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(GAME_MOVE_SPEED);
    }

    /**
     * Resets the player's speed to the default value.
     *
     * @param player the player to reset
     */
    public static void resetSpeed(Player player) {
        player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(DEFAULT_MOVE_SPEED);
    }

    /**
     * Applies the player-count-based health scaling bonus to the player as a modifier on top of the base max health,
     * then heals the player to the resulting max health.
     *
     * @param player the player to update the health scale
     * @param scale  the additional max health to grant
     */
    public static void updateHealthScale(Player player, float scale) {
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        attribute.removeModifier(HEALTH_SCALING_KEY);
        attribute.addModifier(new AttributeModifier(HEALTH_SCALING_KEY, scale, AttributeOperation.ADD_VALUE));
        player.setHealth((float) attribute.getValue());
    }

    /**
     * Removes the health scaling bonus from the player.
     *
     * @param player the player to remove the health scaling from
     */
    public static void removeHealthScale(Player player) {
        player.getAttribute(Attribute.MAX_HEALTH).removeModifier(HEALTH_SCALING_KEY);
    }

    /**
     * Applies the player-count-based speed scaling bonus to the player as a modifier on top of the base movement speed.
     *
     * @param player the player to apply the speed scaling to
     * @param bonus  the additional speed to grant
     */
    public static void updateSpeedScale(Player player, double bonus) {
        AttributeInstance attribute = player.getAttribute(Attribute.MOVEMENT_SPEED);
        attribute.removeModifier(SPEED_SCALING_KEY);
        attribute.addModifier(new AttributeModifier(SPEED_SCALING_KEY, bonus, AttributeOperation.ADD_VALUE));
    }

    /**
     * Removes the speed scaling bonus from the player.
     *
     * @param player the player to remove the speed scaling from
     */
    public static void removeSpeedScale(Player player) {
        player.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(SPEED_SCALING_KEY);
    }

    /**
     * Applies the Slender draining speed modifier to the player.
     *
     * @param player the player to apply the draining speed modifier to
     */
    public static void applySlenderDrainingSpeed(Player player) {
        var attr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        attr.removeModifier(SLENDER_DRAINING_SPEED_KEY);
        attr.addModifier(SLENDER_DRAINING_SPEED_MODIFIER);
    }

    /**
     * Removes the Slender draining speed modifier from the player.
     *
     * @param player the player to remove the draining speed modifier from
     */
    public static void removeSlenderDrainingSpeed(Player player) {
        player.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(SLENDER_DRAINING_SPEED_KEY);
    }

    private AttributeHelper() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
