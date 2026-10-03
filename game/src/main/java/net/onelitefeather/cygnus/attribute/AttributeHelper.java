package net.onelitefeather.cygnus.attribute;

import net.kyori.adventure.key.Key;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeInstance;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;

import java.util.Set;

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
    public static final Key GAME_SPEED_KEY = Key.key("cygnus", "game_speed");
    public static final Key GAME_JUMP_STRENGTH_KEY = Key.key("cygnus", "game_jump_strength");
    public static final Key GAME_STEP_HEIGHT_KEY = Key.key("cygnus", "game_step_height");
    public static final Key FREEZE_KEY = Key.key("cygnus", "freeze");

    private static final AttributeModifier SLENDER_DRAINING_SPEED_MODIFIER = new AttributeModifier(
                    SLENDER_DRAINING_SPEED_KEY,
            -0.331,
            AttributeOperation.ADD_MULTIPLIED_TOTAL
    );

    // Multiplies the final value by zero, so it holds regardless of the other modifiers on the attribute
    private static final AttributeModifier FREEZE_MODIFIER = new AttributeModifier(
            FREEZE_KEY,
            -1.0,
            AttributeOperation.ADD_MULTIPLIED_TOTAL
    );

    // The modifiers that make up the speed a player walks at during a round, as opposed to
    // temporary ones like sprinting or the slender draining
    private static final Set<Key> WALKING_SPEED_KEYS = Set.of(GAME_SPEED_KEY, SPEED_SCALING_KEY);

    private static final double DEFAULT_JUMP_STRENGTH = 0.42;
    private static final double GAME_JUMP_STRENGTH = 0.0;

    private static final double DEFAULT_STEP_HEIGHT = 0.6;
    private static final double GAME_STEP_HEIGHT = 1.0;

    private static final double DEFAULT_MOVE_SPEED = 0.1;
    private static final double GAME_MOVE_SPEED = 0.065;

    private static final AttributeModifier GAME_JUMP_STRENGTH_MODIFIER = new AttributeModifier(
            GAME_JUMP_STRENGTH_KEY,
            GAME_JUMP_STRENGTH - DEFAULT_JUMP_STRENGTH,
            AttributeOperation.ADD_VALUE
    );

    private static final AttributeModifier GAME_STEP_HEIGHT_MODIFIER = new AttributeModifier(
            GAME_STEP_HEIGHT_KEY,
            GAME_STEP_HEIGHT - DEFAULT_STEP_HEIGHT,
            AttributeOperation.ADD_VALUE
    );

    private static final AttributeModifier GAME_SPEED_MODIFIER = new AttributeModifier(
            GAME_SPEED_KEY,
            GAME_MOVE_SPEED - DEFAULT_MOVE_SPEED,
            AttributeOperation.ADD_VALUE
    );


    /**
     * Adjusts the step height and jump strength for the player.
     * The game values are used to prevent the player from jumping but increase the step height.
     *
     * @param player the player to adjust
     */
    public static void adjustStepHeightAndJump(Player player) {
        AttributeInstance jumpStrength = player.getAttribute(Attribute.JUMP_STRENGTH);
        jumpStrength.removeModifier(GAME_JUMP_STRENGTH_KEY);
        jumpStrength.addModifier(GAME_JUMP_STRENGTH_MODIFIER);

        AttributeInstance stepHeight = player.getAttribute(Attribute.STEP_HEIGHT);
        stepHeight.removeModifier(GAME_STEP_HEIGHT_KEY);
        stepHeight.addModifier(GAME_STEP_HEIGHT_MODIFIER);
    }

    /**
     * Resets the step height and jump strength for the player.
     * The default values are used to reset the player's attributes.
     *
     * @param player the player to reset
     */
    public static void resetAttributeAdjustments(Player player) {
        player.getAttribute(Attribute.JUMP_STRENGTH).removeModifier(GAME_JUMP_STRENGTH_KEY);
        player.getAttribute(Attribute.STEP_HEIGHT).removeModifier(GAME_STEP_HEIGHT_KEY);
    }

    /**
     * Decreases the player's speed to the game value.
     *
     * @param player the player to decrease the speed
     */
    public static void decreaseSpeed(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MOVEMENT_SPEED);
        attribute.removeModifier(GAME_SPEED_KEY);
        attribute.addModifier(GAME_SPEED_MODIFIER);
        refreshFieldOfView(player);
    }

    /**
     * Resets the player's speed to the default value.
     *
     * @param player the player to reset
     */
    public static void resetSpeed(Player player) {
        player.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(GAME_SPEED_KEY);
        refreshFieldOfView(player);
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
        refreshFieldOfView(player);
    }

    /**
     * Removes the speed scaling bonus from the player.
     *
     * @param player the player to remove the speed scaling from
     */
    public static void removeSpeedScale(Player player) {
        player.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(SPEED_SCALING_KEY);
        refreshFieldOfView(player);
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

    /**
     * Stops the player from walking and jumping while still letting them look around.
     *
     * @param player the player to freeze
     */
    public static void freeze(Player player) {
        for (AttributeInstance attribute : freezeAttributes(player)) {
            attribute.removeModifier(FREEZE_KEY);
            attribute.addModifier(FREEZE_MODIFIER);
        }
        refreshFieldOfView(player);
    }

    /**
     * Lets a player frozen by {@link #freeze(Player)} move again.
     *
     * @param player the player to release
     */
    public static void unfreeze(Player player) {
        for (AttributeInstance attribute : freezeAttributes(player)) {
            attribute.removeModifier(FREEZE_KEY);
        }
        refreshFieldOfView(player);
    }

    /**
     * Tells the client which speed counts as plain walking, so its FOV only reacts to changes on top of it.
     * <p>
     * The client zooms by {@code movement speed / walking speed}. Left at the vanilla walking speed, the
     * slower game speed reads as a permanent slowness effect and zooms the view in. A frozen player gets a
     * walking speed of zero, which makes the client skip the scaling entirely.
     * </p>
     *
     * @param player the player to update
     */
    private static void refreshFieldOfView(Player player) {
        AttributeInstance speed = player.getAttribute(Attribute.MOVEMENT_SPEED);
        double walkingSpeed = speed.getBaseValue();
        for (AttributeModifier modifier : speed.modifiers()) {
            if (FREEZE_KEY.equals(modifier.id())) {
                player.setFieldViewModifier(0f);
                return;
            }
            if (WALKING_SPEED_KEYS.contains(modifier.id())) {
                walkingSpeed += modifier.amount();
            }
        }
        player.setFieldViewModifier((float) walkingSpeed);
    }

    private static AttributeInstance[] freezeAttributes(Player player) {
        return new AttributeInstance[]{
                player.getAttribute(Attribute.MOVEMENT_SPEED),
                player.getAttribute(Attribute.JUMP_STRENGTH)
        };
    }

    private AttributeHelper() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
