package net.onelitefeather.cygnus.attribute;

import net.kyori.adventure.key.Key;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
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
     * Updates the health scale for the player.
     *
     * @param player the player to update the health scale
     * @param scale  the scale to set
     */
    public static void updateHealthScale(Player player, float scale) {
        float healthScale = (float) (player.getAttribute(Attribute.MAX_HEALTH).getBaseValue() + scale);
        player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(healthScale);
        player.setHealth(healthScale);
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
