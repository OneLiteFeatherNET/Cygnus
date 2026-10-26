package net.onelitefeather.cygnus.creek.consequence;

import net.onelitefeather.cygnus.telemetry.TickSectionNames;
import net.onelitefeather.cygnus.telemetry.TickSections;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Puts a survivor on the slender's locator bar for a few seconds.
 * <p>
 * The waypoint is sent to the slender only, see {@link SurvivorWaypoint}. It follows the survivor
 * with an update every {@value #RESEND_TICKS} ticks. A survivor revealed again while he is still
 * shown keeps his waypoint, and the time starts over. The slender only sees the newest reveal:
 * revealing another survivor takes the one shown before off the locator bar.
 * </p>
 *
 * @author theEvilReaper
 * @version 2.1.0
 * @since 2.15.0
 */
public final class LocatorReveal implements SlenderReveal {

    /** How often the waypoint follows the survivor, in ticks. */
    static final int RESEND_TICKS = 5;

    static final Key SLENDER_SOUND = Key.key("block.bell.use");
    static final Key SURVIVOR_SOUND = Key.key("entity.creaking.activate");
    static final Component WARNING = Component.text("He knows where you are...", NamedTextColor.DARK_RED, TextDecoration.ITALIC);

    private final int durationTicks;
    private final Set<Reveal> running = ConcurrentHashMap.newKeySet();
    private final TickSections sections;

    /**
     * Sets up the reveal with its updates measured for the slow tick report.
     *
     * @param seconds  how long the survivor stays on the locator bar
     * @param sections measures how long the updates take
     * @since 2.15.0
     */
    public LocatorReveal(int seconds, TickSections sections) {
        this.durationTicks = seconds * 20;
        this.sections = sections;
    }

    @Override
    public void reveal(Player survivor, Player slender) {
        Pos where = survivor.getPosition();
        slender.playSound(Sound.sound(SLENDER_SOUND, Sound.Source.HOSTILE, 1.0F, 0.5F), where.x(), where.y(), where.z());
        // Let the survivor know too. Being given away without any hint would feel unfair.
        survivor.playSound(Sound.sound(SURVIVOR_SOUND, Sound.Source.HOSTILE, 1.0F, 0.8F));
        survivor.sendActionBar(WARNING);

        Reveal shown = this.find(survivor, slender);
        if (shown != null) {
            shown.ticksLeft = this.durationTicks;
            return;
        }
        for (Reveal older : List.copyOf(this.running)) {
            if (older.slender == slender) this.end(older);
        }
        Reveal reveal = new Reveal(survivor, slender, this.durationTicks);
        this.running.add(reveal);
        slender.sendPacket(SurvivorWaypoint.track(survivor));
        reveal.task = MinecraftServer.getSchedulerManager()
                .buildTask(this.sections.wrap(TickSectionNames.LOCATOR_REVEAL, () -> this.step(reveal)))
                .delay(TaskSchedule.tick(RESEND_TICKS))
                .repeat(TaskSchedule.tick(RESEND_TICKS))
                .schedule();
    }

    private @Nullable Reveal find(Player survivor, Player slender) {
        for (Reveal reveal : this.running) {
            if (reveal.survivor == survivor && reveal.slender == slender) return reveal;
        }
        return null;
    }

    private void step(Reveal reveal) {
        reveal.ticksLeft -= RESEND_TICKS;
        if (reveal.ticksLeft <= 0 || !reveal.survivor.isOnline() || !reveal.slender.isOnline()) {
            this.end(reveal);
            return;
        }
        reveal.slender.sendPacket(SurvivorWaypoint.update(reveal.survivor));
    }

    @Override
    public void cleanUp() {
        for (Reveal reveal : List.copyOf(this.running)) {
            this.end(reveal);
        }
    }

    private void end(Reveal reveal) {
        if (!this.running.remove(reveal)) return;
        if (reveal.task != null) reveal.task.cancel();
        if (reveal.slender.isOnline()) {
            reveal.slender.sendPacket(SurvivorWaypoint.untrack(reveal.survivor));
        }
    }

    private static final class Reveal {

        private final Player survivor;
        private final Player slender;
        private int ticksLeft;
        private @Nullable Task task;

        private Reveal(Player survivor, Player slender, int ticksLeft) {
            this.survivor = survivor;
            this.slender = slender;
            this.ticksLeft = ticksLeft;
        }
    }
}
