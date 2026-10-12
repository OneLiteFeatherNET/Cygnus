package net.onelitefeather.cygnus.creek.tab;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.creek.dread.CreekWitness;
import net.onelitefeather.cygnus.creek.dread.HuntEnd;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Marks the survivors a creek is hunting in the tab list, and takes the mark away when the hunt is over.
 * <p>
 * It decorates the {@link CreekWitness}, so the creek needs no knowledge of the tab list. The mark is
 * composed on top of whatever display name the survivor carries (role icon, name color) instead of
 * replacing it: the name turns {@link NamedTextColor#RED} and a {@value #MARKER} marker goes behind it, after a space.
 * Glyphs from the icon font keep their own color.
 * </p>
 * <p>
 * The display name from before is kept and put back exactly, but only if the survivor still shows the
 * marked name. If anything else renamed them in the meantime (a spectator's struck-through name, the
 * rank tag of the restart lobby), that newer name wins and nothing is restored over it. Two creeks on
 * one survivor are counted, so the mark stays until the last hunt ends.
 * </p>
 * <p>
 * The display name is global, so every player sees the mark in the tab list, not only the hunted one.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.2.0
 * @since 2.15.0
 */
public final class HuntedTabWitness implements CreekWitness {

    /** The marker behind a hunted survivor's name. */
    public static final String MARKER = "◆";

    private final CreekWitness delegate;
    private final Function<UUID, @Nullable Player> players;
    private final Map<UUID, Mark> marks = new ConcurrentHashMap<>();

    /**
     * Creates the decorator.
     *
     * @param delegate the witness that still has to hear everything
     * @param players  finds an online player by id, or {@code null} if they are gone
     */
    public HuntedTabWitness(CreekWitness delegate, Function<UUID, @Nullable Player> players) {
        this.delegate = delegate;
        this.players = players;
    }

    /**
     * Builds the marked version of a display name.
     *
     * @param base the name as it is shown without the mark
     * @return the red name with the marker behind it
     */
    public static Component highlight(Component base) {
        return Component.text()
                .color(NamedTextColor.RED)
                .append(recolor(base))
                .append(Component.text(" " + MARKER, NamedTextColor.DARK_RED))
                .build();
    }

    /**
     * Turns every colored text red, except the icon glyphs, which are full-color bitmaps.
     */
    private static Component recolor(Component component) {
        Component out = component;
        if (component.font() == null && component.color() != null) out = out.color(NamedTextColor.RED);
        List<Component> children = component.children().stream().map(HuntedTabWitness::recolor).toList();
        return out.children(children);
    }

    @Override
    public void hunted(UUID survivor) {
        this.delegate.hunted(survivor);
        Player player = this.players.apply(survivor);
        if (player == null) return;
        this.marks.compute(survivor, (_, mark) -> {
            if (mark != null) return mark.another();
            Component base = player.getDisplayName();
            Component shown = highlight(base != null ? base : Component.text(player.getUsername()));
            player.setDisplayName(shown);
            return new Mark(base, shown, 1);
        });
    }

    @Override
    public void huntEnded(UUID survivor, HuntEnd how) {
        this.delegate.huntEnded(survivor, how);
        this.unmark(survivor);
    }

    @Override
    public void huntEnded(UUID survivor) {
        this.delegate.huntEnded(survivor);
        this.unmark(survivor);
    }

    private void unmark(UUID survivor) {
        this.marks.computeIfPresent(survivor, (_, mark) -> {
            if (mark.hunts() > 1) return mark.fewer();
            Player player = this.players.apply(survivor);
            if (player != null && mark.shown().equals(player.getDisplayName())) {
                player.setDisplayName(mark.base());
            }
            return null;
        });
    }

    @Override
    public void sighted(UUID survivor) {
        this.delegate.sighted(survivor);
    }

    @Override
    public void caught(UUID survivor) {
        this.delegate.caught(survivor);
    }

    @Override
    public void stalked(UUID survivor) {
        this.delegate.stalked(survivor);
    }

    @Override
    public void selected(UUID survivor) {
        this.delegate.selected(survivor);
    }

    /**
     * A survivor's name before the mark, the marked name, and how many creeks hunt them.
     */
    private record Mark(@Nullable Component base, Component shown, int hunts) {
        Mark another() {
            return new Mark(this.base, this.shown, this.hunts + 1);
        }

        Mark fewer() {
            return new Mark(this.base, this.shown, this.hunts - 1);
        }
    }
}
