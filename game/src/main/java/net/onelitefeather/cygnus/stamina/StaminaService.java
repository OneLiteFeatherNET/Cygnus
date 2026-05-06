package net.onelitefeather.cygnus.stamina;

import net.theevilreaper.xerus.api.team.Team;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.validate.Check;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The class has some abilities to manage all {@link StaminaBar} references which are required in the game.
 *
 * @author theEvilReaper
 * @version 1.1.0
 * @since 1.0.0
 */
public final class StaminaService {

    private final Map<UUID, StaminaBar> staminaBars;
    private @Nullable StaminaBar slenderBar;

    /**
     * Creates a new instance from this class.
     */
    public StaminaService() {
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

        this.slenderBar = StaminaFactory.createSlenderStamina((CygnusPlayer) player);
        if (!forceStart) return;
        this.slenderBar.start();
    }

    /**
     * Creates for each player on a team a new instance from an {@link FoodBar}.
     *
     * @param team the team to get the player from it
     */
    public void createStaminaBars(Team team) {
        Check.argCondition(!staminaBars.isEmpty(), "Unable to load stamina bars twice");
        Check.argCondition(team.getPlayers().isEmpty(), "Can't add players from a team without teams");
        for (Player player : team.getPlayers()) {
            this.staminaBars.put(player.getUuid(), StaminaFactory.createFoodStamina((CygnusPlayer) player));
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
     * Force stops the internal dispatch logic of the bar.
     */
    public void forceStopSlenderBar() {
        if (this.slenderBar == null) return;
        this.slenderBar.stop();
        this.slenderBar = null;
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
