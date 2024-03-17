package net.onelitefeather.cygnus.stamina;

import de.icevizion.xerus.api.team.Team;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.PacketUtils;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The class has some abilities to manage all {@link StaminaBar} references which are required in the game.
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public final class StaminaService {

    private final Map<UUID, StaminaBar> staminaBars;
    private StaminaBar slenderBar;

    /**
     * Creates a new instance from this class.
     */
    public StaminaService() {
        this.staminaBars = new HashMap<>();
    }

    /**
     * Creates a new instance of an {@link SlenderBar} for a given {@link Player}.
     * @param player the player which owns the {@link StaminaBar}
     */
    public void setSlenderBar(@NotNull Player player) {
        this.slenderBar = StaminaFactory.createSlenderStamina(player);
    }

    /**
     * Creates for each player in a team a new instance from an {@link FoodBar}.
     * @param team the team to get the player from it
     */
    public void createStaminaBars(@NotNull Team team) {
        Check.argCondition(!staminaBars.isEmpty(), "Unable to load stamina bars twice");
        Check.argCondition(team.getPlayers().isEmpty(), "Can't add players from a team without teams");
        ((SlenderBar)this.slenderBar).setAccept((player, status) -> {
            if (status == StaminaBar.Status.DRAINING) {
                PacketUtils.broadcastPacket(player.getMetadataPacket());
                MinecraftServer.getConnectionManager().getOnlinePlayers().stream().filter(p -> !p.getUuid().equals(player.getUuid())).forEach(p -> {
                    player.updateNewViewer(p);
                });
                PacketUtils.broadcastPacket(player.getMetadataPacket());
                return null;
            }

            if (status == StaminaBar.Status.REGENERATING) {
                PacketUtils.broadcastPacket(player.getMetadataPacket());
                MinecraftServer.getConnectionManager().getOnlinePlayers().stream().filter(p -> !p.getUuid().equals(player.getUuid())).forEach(p -> {
                    player.updateOldViewer(p);
                });
                PacketUtils.broadcastPacket(player.getMetadataPacket());
                return null;
            }
            return null;
        });
        for (Player player : team.getPlayers()) {
            this.staminaBars.put(player.getUuid(), StaminaFactory.createFoodStamina(player));
        }
    }

    /**
     * Starts all {@link net.minestom.server.timer.Task} reference from each {@link StaminaBar}.
     */
    public void start() {
        this.slenderBar.start();
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

    public void switchToSlenderBar(@NotNull Player player) {
        this.slenderBar = StaminaFactory.createSlenderStamina(player);
        this.slenderBar.start();
    }

    public void forceStopSlenderBar() {
        this.slenderBar.stop();
        this.slenderBar = null;
    }

    public @NotNull FoodBar getFoodBar(@NotNull Player player) {
        return (FoodBar) this.staminaBars.get(player.getUuid());
    }

    public @Nullable StaminaBar getSlenderBar() {
        return slenderBar;
    }
}
