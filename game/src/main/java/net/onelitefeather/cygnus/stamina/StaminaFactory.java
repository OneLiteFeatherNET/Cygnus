package net.onelitefeather.cygnus.stamina;

import net.onelitefeather.cygnus.player.CygnusPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * The StaminaFactory class provides a convenient and flexible mechanism for creating instances of the {@link StaminaBar} class,
 * allowing game developers to customize and manage stamina bars for players.
 * This factory encapsulates the process of constructing {@link StaminaBar} objects, simplifying their initialization.
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
public abstract class StaminaFactory {

    private StaminaFactory() { }

    /**
     * Creates a new instance of an {@link FoodBar}.
     * @param player the player who owns the created object
     * @return the created instance from a {@link FoodBar}
     */
    public static @NotNull StaminaBar createFoodStamina(@NotNull CygnusPlayer player) {
        return new FoodBar(player);
    }

    /**
     * Creates a new instance of an {@link SlenderBar}.
     * @param player the player who owns the created object
     * @return the created instance from a {@link SlenderBar}
     */
    public static @NotNull StaminaBar createSlenderStamina(@NotNull CygnusPlayer player) {
        return new SlenderBar(player);
    }
}
