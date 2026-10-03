package net.onelitefeather.cygnus.stamina;

import net.onelitefeather.cygnus.common.config.StaminaConfig;
import net.onelitefeather.cygnus.player.CygnusPlayer;

import java.util.function.LongSupplier;

/**
 * The StaminaFactory class provides a convenient and flexible mechanism for creating instances of the {@link StaminaBar} class,
 * allowing game developers to customize and manage stamina bars for players.
 * This factory encapsulates the process of constructing {@link StaminaBar} objects, simplifying their initialization.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
public final class StaminaFactory {

    private StaminaFactory() {
    }

    /**
     * Creates a new instance of an {@link FoodBar} with the default settings.
     *
     * @param player the player who owns the created object
     * @return the created instance from a {@link FoodBar}
     */
    public static StaminaBar createFoodStamina(CygnusPlayer player) {
        return createFoodStamina(player, StaminaConfig.DEFAULT);
    }

    /**
     * Creates a new instance of an {@link FoodBar}.
     *
     * @param player the player who owns the created object
     * @param config the sprint settings
     * @return the created instance from a {@link FoodBar}
     */
    public static StaminaBar createFoodStamina(CygnusPlayer player, StaminaConfig config) {
        return new FoodBar(player, config);
    }

    /**
     * Creates a new instance of an {@link SlenderBar} with the default settings and the system clock.
     *
     * @param player the player who owns the created object
     * @return the created instance from a {@link SlenderBar}
     */
    public static StaminaBar createSlenderStamina(CygnusPlayer player) {
        return createSlenderStamina(player, StaminaConfig.DEFAULT, System::currentTimeMillis);
    }

    /**
     * Creates a new instance of an {@link SlenderBar}.
     *
     * @param player the player who owns the created object
     * @param config the settings, of which the bar reads the reappear cooldown
     * @param clock  supplies the current time in milliseconds
     * @return the created instance from a {@link SlenderBar}
     */
    public static StaminaBar createSlenderStamina(CygnusPlayer player, StaminaConfig config, LongSupplier clock) {
        return new SlenderBar(player, config, clock);
    }
}
