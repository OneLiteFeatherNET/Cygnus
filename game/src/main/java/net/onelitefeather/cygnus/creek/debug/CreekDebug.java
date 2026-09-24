package net.onelitefeather.cygnus.creek.debug;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.creek.state.CreekState;
import net.onelitefeather.cygnus.creek.state.HuntState;
import net.onelitefeather.cygnus.creek.state.StalkState;
import net.onelitefeather.cygnus.creek.state.SurvivorView;
import net.onelitefeather.cygnus.creek.state.VanishState;
import net.onelitefeather.cygnus.creek.state.WanderState;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Shows what the creek is doing in the action bar of players who turned it on. Meant for
 * playtests.
 * <p>
 * Watchers see everything, whatever their role. This is a tuning tool, not something for
 * players in a real round.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class CreekDebug {

    /** Shown to watchers while no round is running. */
    public static final Component INACTIVE = Component.text("Creek inactive", NamedTextColor.GRAY);

    /** Marks a survivor who currently sees the creek. The default Minecraft font has no emoji. */
    static final String SEEN = "◉";

    private final Set<UUID> watchers = ConcurrentHashMap.newKeySet();
    private volatile boolean active;

    /**
     * Turns the debug line on or off for a player.
     *
     * @param player the player's id
     * @return {@code true} if the line is now on
     */
    public boolean toggle(UUID player) {
        if (this.watchers.remove(player)) return false;
        this.watchers.add(player);
        return true;
    }

    /**
     * Returns whether anyone has the debug line turned on.
     *
     * @return {@code true} if at least one player watches
     */
    public boolean hasWatchers() {
        return !this.watchers.isEmpty();
    }

    /**
     * Returns whether the creek is currently in a round.
     *
     * @return {@code true} while the creek is in the world
     */
    public boolean isActive() {
        return this.active;
    }

    /**
     * Sets whether the creek is in a round. When it leaves, watchers see "Creek inactive".
     *
     * @param active {@code true} while the creek is in the world
     */
    public void setActive(boolean active) {
        this.active = active;
        if (!active) this.show(INACTIVE);
    }

    /**
     * Sends a line to every online watcher.
     *
     * @param line the line to show
     */
    public void show(Component line) {
        for (UUID id : this.watchers) {
            Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(id);
            if (player != null) player.sendActionBar(line);
        }
    }

    /**
     * Builds the debug line for one step.
     *
     * @param state the creek's state
     * @param creek the creek's position
     * @param views the survivors in that step
     * @param names turns a player id into a name
     * @return the line
     */
    public static Component line(CreekState state, Pos creek, List<SurvivorView> views, Function<UUID, String> names) {
        TextComponent.Builder builder = Component.text();
        builder.append(Component.text(label(state), NamedTextColor.RED));

        target(state).ifPresent(id -> {
            builder.append(Component.text(" → " + names.apply(id), NamedTextColor.GOLD));
            views.stream()
                    .filter(view -> view.id().equals(id))
                    .findFirst()
                    .ifPresent(view -> builder.append(Component.text(
                            String.format(Locale.ROOT, " · %.1f m", view.position().distance(creek)),
                            NamedTextColor.GRAY)));
        });

        for (SurvivorView view : views) {
            builder.append(Component.text(" · ", NamedTextColor.DARK_GRAY));
            builder.append(Component.text(
                    String.format(Locale.ROOT, "%s %.2f", names.apply(view.id()), view.dread()),
                    NamedTextColor.GRAY));
            if (view.seesCreek()) {
                builder.append(Component.text(" " + SEEN, NamedTextColor.YELLOW));
            }
        }
        return builder.build();
    }

    /**
     * Returns the display name of a state.
     *
     * @param state the state
     * @return the name, for example {@code HUNT}
     */
    public static String label(CreekState state) {
        return switch (state) {
            case WanderState _ -> "WANDER";
            case StalkState _ -> "STALK";
            case HuntState _ -> "HUNT";
            case VanishState _ -> "VANISH";
            default -> state.getClass().getSimpleName();
        };
    }

    private static Optional<UUID> target(CreekState state) {
        return switch (state) {
            case StalkState stalk -> Optional.of(stalk.target());
            case HuntState hunt -> Optional.of(hunt.target());
            default -> Optional.empty();
        };
    }
}
