package net.onelitefeather.cygnus.minimap;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.onelitefeather.cygnus.GameFeature;
import net.onelitefeather.cygnus.common.config.MinimapConfig;

import java.util.List;

/**
 * Asks cooperating client minimap mods to switch themselves off.
 * <p>
 * The mods listen to the system chat for lines made of nothing but colour codes. Such a line renders as
 * an empty message, yet the mod reads the raw text and obeys it. The codes are sent as literal text, so
 * the {@code §} characters reach the client untouched:
 * </p>
 * <ul>
 *     <li>Xaero's Minimap / World Map: {@value #XAERO_FAIR_PLAY} (no entity radar, no cave mode) and
 *     {@value #XAERO_NO_MINIMAP} (no minimap at all).</li>
 *     <li>VoxelMap and mods that copy its convention: {@value #VOXEL_NO_RADAR} (no radar) and
 *     {@value #VOXEL_NO_CAVES} (no cave mode). There is no code that switches VoxelMap off completely.</li>
 * </ul>
 * <p>
 * This is opt-in on the mods' side and can not be enforced: a client that ignores the lines keeps its
 * minimap. The lines are sent on every {@link PlayerSpawnEvent}, that is on the first spawn and whenever
 * the player spawns into another instance, so a mod that forgets its flags on a world change is told
 * again. JourneyMap is not covered, it takes its rules from a server mod.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
public final class MinimapPolicy implements GameFeature {

    /** Xaero: switch off the entity radar and the cave mode. */
    public static final String XAERO_FAIR_PLAY = "§f§a§i§r§x§a§e§r§o";

    /** Xaero: switch off the minimap entirely. */
    public static final String XAERO_NO_MINIMAP = "§n§o§m§i§n§i§m§a§p";

    /** VoxelMap: switch off the radar. */
    public static final String VOXEL_NO_RADAR = "§3 §6 §3 §6 §3 §6 §e";

    /** VoxelMap: switch off the cave mode. */
    public static final String VOXEL_NO_CAVES = "§3 §6 §3 §6 §3 §6 §d";

    private final EventNode<Event> node = EventNode.all("minimap");
    private final MinimapConfig config;

    /**
     * Creates the policy.
     *
     * @param config the mode to ask the mods for
     */
    public MinimapPolicy(MinimapConfig config) {
        this.config = config;
        this.node.addListener(PlayerSpawnEvent.class, event -> this.apply(event.getPlayer()));
    }

    /**
     * Returns the lines a mode sends, in the order they are sent.
     *
     * @param mode the mode
     * @return the raw lines, empty for {@link MinimapConfig.Mode#OFF}
     */
    public static List<String> codes(MinimapConfig.Mode mode) {
        return switch (mode) {
            case DISABLED -> List.of(XAERO_NO_MINIMAP, XAERO_FAIR_PLAY, VOXEL_NO_RADAR, VOXEL_NO_CAVES);
            case FAIR -> List.of(XAERO_FAIR_PLAY, VOXEL_NO_RADAR, VOXEL_NO_CAVES);
            case OFF -> List.of();
        };
    }

    /**
     * Sends the lines of the configured mode to a player, one system message each: Xaero compares a
     * whole message with its code, so the lines must not be joined.
     *
     * @param player the player to ask
     */
    public void apply(Player player) {
        for (String code : codes(this.config.mode())) {
            player.sendMessage(Component.text(code));
        }
    }

    @Override
    public EventNode<Event> node() {
        return this.node;
    }

    @Override
    public boolean enabled() {
        return this.config.mode() != MinimapConfig.Mode.OFF;
    }
}
