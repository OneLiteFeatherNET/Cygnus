package net.onelitefeather.cygnus.creek.consequence;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Metadata;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lets a survivor glow for a few seconds, but only in the slender's eyes.
 * <p>
 * Glowing is a metadata flag, and Minestom sends metadata to everyone who can see the player. So
 * the flag is never set on the survivor itself. Instead, only the slender gets a packet with it.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class GlowReveal implements SlenderReveal {

    /** How often the glow packet goes out again, in ticks. */
    static final int RESEND_TICKS = 5;

    /** The glowing bit in the entity flags. */
    static final byte GLOWING = 0x40;

    static final Key SLENDER_SOUND = Key.key("block.bell.use");
    static final Key SURVIVOR_SOUND = Key.key("entity.creaking.activate");
    static final Component WARNING = Component.text("He knows where you are...", NamedTextColor.DARK_RED, TextDecoration.ITALIC);

    private final int durationTicks;
    private final Set<Reveal> running = ConcurrentHashMap.newKeySet();

    /**
     * Sets up the reveal.
     *
     * @param seconds how long the glow lasts
     */
    public GlowReveal(int seconds) {
        this.durationTicks = seconds * 20;
    }

    @Override
    public void reveal(Player survivor, Player slender) {
        Pos where = survivor.getPosition();
        slender.playSound(Sound.sound(SLENDER_SOUND, Sound.Source.HOSTILE, 1.0F, 0.5F), where.x(), where.y(), where.z());
        // Let the survivor know too. Being given away without any hint would feel unfair.
        survivor.playSound(Sound.sound(SURVIVOR_SOUND, Sound.Source.HOSTILE, 1.0F, 0.8F));
        survivor.sendMessage(WARNING);

        Reveal reveal = new Reveal(survivor, slender, this.durationTicks);
        this.running.add(reveal);
        slender.sendPacket(flagsPacket(survivor, true));
        reveal.task = MinecraftServer.getSchedulerManager()
                .buildTask(() -> this.step(reveal))
                .delay(TaskSchedule.tick(RESEND_TICKS))
                .repeat(TaskSchedule.tick(RESEND_TICKS))
                .schedule();
    }

    private void step(Reveal reveal) {
        reveal.ticksLeft -= RESEND_TICKS;
        if (reveal.ticksLeft <= 0 || !reveal.survivor.isOnline() || !reveal.slender.isOnline()) {
            this.end(reveal);
            return;
        }
        // Whenever the survivor's metadata changes (sprinting, sneaking), the slender gets the real
        // flags and the glow is gone. Sending it again keeps that gap short.
        reveal.slender.sendPacket(flagsPacket(reveal.survivor, true));
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
            reveal.slender.sendPacket(flagsPacket(reveal.survivor, false));
        }
    }

    /**
     * Builds a packet with the survivor's current entity flags, with the glow bit set or cleared.
     * <p>
     * The flags always go along. Otherwise the client would hold on to the last value it got, which
     * after a reveal is the glow.
     * </p>
     *
     * @param survivor the survivor whose flags are sent
     * @param glowing  whether the glow bit is set
     * @return the packet, meant for the slender only
     */
    public static EntityMetaDataPacket flagsPacket(Player survivor, boolean glowing) {
        Metadata.Entry<?> entry = survivor.getMetadataPacket().entries().get(0);
        byte flags = entry != null && entry.value() instanceof Byte value ? value : 0;
        byte sent = glowing ? (byte) (flags | GLOWING) : (byte) (flags & ~GLOWING);
        return new EntityMetaDataPacket(survivor.getEntityId(), Map.of(0, Metadata.Byte(sent)));
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
