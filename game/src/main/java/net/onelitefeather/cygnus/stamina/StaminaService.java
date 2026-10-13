package net.onelitefeather.cygnus.stamina;

import net.onelitefeather.cygnus.telemetry.TickSectionNames;
import net.onelitefeather.cygnus.telemetry.TickSections;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.validate.Check;
import net.onelitefeather.cygnus.common.config.StaminaConfig;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The class has some abilities to manage all {@link StaminaBar} references which are required in the game.
 *
 * @author theEvilReaper
 * @version 1.2.0
 * @since 1.0.0
 */
public final class StaminaService {

    private final StaminaConfig config;
    private final TickSections sections;
    private final Map<UUID, StaminaBar> staminaBars;
    private @Nullable StaminaBar slenderBar;

    /**
     * Creates a new instance from this class with the default settings.
     */
    public StaminaService() {
        this(StaminaConfig.DEFAULT);
    }

    /**
     * Creates a new instance from this class.
     *
     * @param config the settings every bar is created with
     */
    public StaminaService(StaminaConfig config) {
        this(config, TickSections.NONE);
    }

    /**
     * Creates a new instance from this class with its bars measured for the slow tick report.
     *
     * @param config   the settings every bar is created with
     * @param sections measures how long the bars take, the survivors' together and the slender's apart
     * @since 2.15.0
     */
    public StaminaService(StaminaConfig config, TickSections sections) {
        this.config = config;
        this.sections = sections;
        this.staminaBars = new HashMap<>();
    }

    /**
     * Creates a new instance of an {@link SlenderBar} for a given {@link Player}.
     *
     * @param player the player that owns the {@link StaminaBar}
     */
    public void setSlenderBar(Player player) {
        this.setSlenderBar(player, false);
    }

    /**
     * Creates a new instance of an {@link SlenderBar} for a given {@link Player}.
     *
     * @param player     the player that owns the {@link StaminaBar}
     * @param forceStart if the bar should be started by default
     */
    public void setSlenderBar(Player player, boolean forceStart) {
        if (this.slenderBar != null) {
            this.slenderBar.stop();
        }

        this.slenderBar = StaminaFactory.createSlenderStamina((CygnusPlayer) player, this.config, System::currentTimeMillis)
                .measuredBy(this.sections, TickSectionNames.SLENDER_BAR);
        if (!forceStart) return;
        this.slenderBar.start();
    }

    /**
     * Creates for each player on a team a new instance from an {@link FoodBar}.
     *
     * @param team the team to get the player from it
     */
    public void createStaminaBars(Set<Player> team) {
        Check.argCondition(!staminaBars.isEmpty(), "Unable to load stamina bars twice");
        Check.argCondition(team.isEmpty(), "Can't add players from a team without teams");
        for (Player player : team) {
            this.staminaBars.put(player.getUuid(), StaminaFactory.createFoodStamina((CygnusPlayer) player, this.config)
                    .measuredBy(this.sections, TickSectionNames.STAMINA));
        }
    }

    /**
     * Starts all {@link net.minestom.server.timer.Task} reference from each {@link StaminaBar}.
     */
    public void start() {
        for (StaminaBar value : this.staminaBars.values()) {
            value.start();
        }
    }

    /**
     * Stops all running {@link StaminaBar} instances.
     */
    public void cleanUp() {
        if (this.slenderBar == null && staminaBars.isEmpty()) return;

        if (slenderBar != null) {
            this.slenderBar.stop();
            this.slenderBar = null;
        }

        for (StaminaBar value : staminaBars.values()) {
            value.stop();
        }
        staminaBars.clear();
    }

    /**
     * Stops and removes all stamina bars associated with the given player.
     *
     * @param player the player whose stamina bars should be removed
     */
    public void removePlayer(Player player) {
        StaminaBar bar = this.staminaBars.remove(player.getUuid());
        if (bar != null) {
            bar.stop();
        }
        if (this.slenderBar != null && this.slenderBar.player.equals(player)) {
            this.slenderBar.stop();
            this.slenderBar = null;
        }
    }

    /**
     * Returns an instance of a {@link FoodBar} from a given player
     *
     * @param player to get the bar
     * @return the corresponding {@link FoodBar} instance
     */
    public FoodBar getFoodBar(Player player) {
        return (FoodBar) this.staminaBars.get(player.getUuid());
    }

    /**
     * Returns the current reference of a {@link SlenderBar} if it exists
     *
     * @return current reference
     */
    public @Nullable StaminaBar getSlenderBar() {
        return slenderBar;
    }
}
