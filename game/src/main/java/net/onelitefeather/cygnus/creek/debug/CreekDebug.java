package net.onelitefeather.cygnus.creek.debug;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.creek.state.CreekState;
import net.onelitefeather.cygnus.creek.state.DoneState;
import net.onelitefeather.cygnus.creek.state.HuntState;
import net.onelitefeather.cygnus.creek.state.PatrolState;
import net.onelitefeather.cygnus.creek.state.StalkState;
import net.onelitefeather.cygnus.creek.state.SurvivorView;
import net.onelitefeather.cygnus.creek.state.VanishState;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Shows what the creek is up to in the action bar of everyone who switched it on. Meant for
 * playtests.
 * <p>
 * Watchers see everything, whatever their role in the round. It is a tuning tool, not something
 * players should have during a real game.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class CreekDebug {

    /** What watchers see while no round is running. */
    public static final Component INACTIVE = Component.text("Creek inactive", NamedTextColor.GRAY);

    /** Marks a survivor who can see the creek right now. The default Minecraft font has no emoji. */
    static final String SEEN = "◉";

    private final Set<UUID> watchers = ConcurrentHashMap.newKeySet();
    private volatile boolean active;

    /**
     * Switches the debug line on or off for a player.
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
     * Tells whether anyone has the debug line switched on.
     *
     * @return {@code true} if at least one player is watching
     */
    public boolean hasWatchers() {
        return !this.watchers.isEmpty();
    }

    /**
     * Tells whether the creek is part of a round right now.
     *
     * @return {@code true} while the creek is in the world
     */
    public boolean isActive() {
        return this.active;
    }

    /**
     * Tells the debug line whether the creek is part of a round. Once it leaves, watchers see
     * "Creek inactive".
     *
     * @param active {@code true} while the creek is in the world
     */
    public void setActive(boolean active) {
        this.active = active;
        if (!active) this.show(INACTIVE);
    }

    /**
     * Sends a line to every watcher who is online.
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
     * @param creek where the creek is
     * @param views the survivors in that step
     * @param names turns a player's id into a name
     * @return the line
     */
    public static Component line(CreekState state, Pos creek, List<SurvivorView> views, Function<UUID, String> names) {
        return line(state, creek, views, names, "");
    }

    /**
     * Builds the debug line for one step, together with where the creek is on its route.
     *
     * @param state the creek's state
     * @param creek where the creek is
     * @param views the survivors in that step
     * @param names turns a player's id into a name
     * @param route where the creek is on its route, or an empty string without a route
     * @return the line
     */
    public static Component line(CreekState state, Pos creek, List<SurvivorView> views, Function<UUID, String> names,
                                 String route) {
        return line(state, creek, views, names, route, Long.MAX_VALUE);
    }

    /**
     * Builds the debug line of the patrolling creek.
     *
     * @param state the creek's state
     * @param creek where the creek is
     * @param views every survivor
     * @param names turns a survivor's id into a name
     * @param route where the creek is on its route, or an empty string
     * @param now   the current time in milliseconds, to show how long until it picks someone out again
     * @return the line
     */
    public static Component line(CreekState state, Pos creek, List<SurvivorView> views, Function<UUID, String> names,
                                 String route, long now) {
        TextComponent.Builder builder = Component.text();
        builder.append(Component.text(label(state), NamedTextColor.RED));
        if (!route.isEmpty()) {
            builder.append(Component.text(" · " + route, NamedTextColor.AQUA));
        }

        target(state).ifPresent(id -> {
            builder.append(Component.text(" → " + names.apply(id), NamedTextColor.GOLD));
            views.stream()
                    .filter(view -> view.id().equals(id))
                    .findFirst()
                    .ifPresent(view -> builder.append(Component.text(
                            String.format(Locale.ROOT, " · %.1f m", view.position().distance(creek)),
                            NamedTextColor.GRAY)));
        });

        if (state instanceof PatrolState patrol && patrol.selectAllowedAt() > now) {
            long seconds = (patrol.selectAllowedAt() - now + 999L) / 1000L;
            builder.append(Component.text(" · select " + seconds + "s", NamedTextColor.GRAY));
        }

        for (SurvivorView view : views) {
            builder.append(Component.text(" · ", NamedTextColor.DARK_GRAY));
            // Shown as sanity, the inverse of the dread: 1 is calm, the lower the more scared.
            builder.append(Component.text(
                    String.format(Locale.ROOT, "%s %.2f", names.apply(view.id()), 1.0D - view.dread()),
                    NamedTextColor.GRAY));
            if (view.seesCreek()) {
                builder.append(Component.text(" " + SEEN, NamedTextColor.YELLOW));
            }
        }
        return builder.build();
    }

    /**
     * Builds the part of the debug line about the variants: how many are running, and for each
     * one who it haunts, what it is doing and how many seconds it has left.
     *
     * @param states   the states of the running variants
     * @param capacity how many variants may run at once
     * @param names    turns a survivor's id into a name
     * @param now      the current time in milliseconds
     * @return the segment, starting with a separator
     */
    public static Component variants(List<CreekState> states, int capacity, Function<UUID, String> names, long now) {
        TextComponent.Builder builder = Component.text();
        builder.append(Component.text(" | ", NamedTextColor.DARK_GRAY));
        builder.append(Component.text("variants " + states.size() + "/" + capacity, NamedTextColor.LIGHT_PURPLE));
        for (CreekState state : states) {
            long endsAt = switch (state) {
                case StalkState stalk -> stalk.endsAt();
                case HuntState hunt -> hunt.endsAt();
                default -> now;
            };
            String who = target(state).map(names).orElse("?");
            long seconds = Math.max(0L, (endsAt - now) / 1000L);
            builder.append(Component.text(" · " + who + " " + label(state) + " " + seconds + "s", NamedTextColor.GRAY));
        }
        return builder.build();
    }

    /**
     * The name a state goes by in the debug line.
     *
     * @param state the state
     * @return the name, for example {@code HUNT}
     */
    public static String label(CreekState state) {
        return switch (state) {
            case PatrolState _ -> "PATROL";
            case StalkState _ -> "STALK";
            case HuntState _ -> "HUNT";
            case VanishState _ -> "VANISH";
            case DoneState _ -> "DONE";
            default -> state.getClass().getSimpleName();
        };
    }

    private static Optional<UUID> target(CreekState state) {
        return switch (state) {
            case StalkState stalk -> Optional.of(stalk.target());
            case HuntState hunt -> Optional.of(hunt.target());
            case PatrolState patrol -> patrol.staring();
            default -> Optional.empty();
        };
    }
}
