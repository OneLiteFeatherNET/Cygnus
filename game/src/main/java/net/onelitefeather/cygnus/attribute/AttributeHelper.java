package net.onelitefeather.cygnus.attribute;

import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import org.jetbrains.annotations.NotNull;

/**
 * The {@link AttributeHelper} class provides utility methods to adjust the player's attributes.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
@SuppressWarnings("java:S3252")
public final class AttributeHelper {

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
    public static void adjustStepHeightAndJump(@NotNull Player player) {
        player.getAttribute(Attribute.JUMP_STRENGTH).setBaseValue(GAME_JUMP_STRENGTH);
        player.getAttribute(Attribute.STEP_HEIGHT).setBaseValue(GAME_STEP_HEIGHT);
    }

    /**
     * Resets the step height and jump strength for the player.
     * The default values are used to reset the player's attributes.
     *
     * @param player the player to reset
     */
    public static void resetAttributeAdjustments(@NotNull Player player) {
        player.getAttribute(Attribute.JUMP_STRENGTH).setBaseValue(DEFAULT_JUMP_STRENGTH);
        player.getAttribute(Attribute.STEP_HEIGHT).setBaseValue(DEFAULT_STEP_HEIGHT);
    }

    /**
     * Increases the player's speed to the game value.
     *
     * @param player the player to increase the speed
     */
    public static void decreaseSpeed(@NotNull Player player) {
        player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(GAME_MOVE_SPEED);
    }

    /**
     * Resets the player's speed to the default value.
     *
     * @param player the player to reset
     */
    public static void resetSpeed(@NotNull Player player) {
        player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(DEFAULT_MOVE_SPEED);
    }

    /**
     * Updates the health scale for the player.
     *
     * @param player the player to update the health scale
     * @param scale  the scale to set
     */
    public static void updateHealthScale(@NotNull Player player, float scale) {
        float healthScale = (float) (player.getAttribute(Attribute.MAX_HEALTH).getBaseValue() + scale);
        player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(healthScale);
        player.setHealth(healthScale);
    }

    private AttributeHelper() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
