package net.onelitefeather.cygnus.phase.task;

import net.kyori.adventure.sound.Sound;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.sound.SoundEvent;

import java.util.Map;

/**
 * Plays a dissonant, descending note-block motif at fixed countdown marks during the
 * last seconds of the {@link net.onelitefeather.cygnus.phase.LobbyPhase}, building
 * tension toward the match start.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public final class LobbyCountdownSoundTask {

    /**
     * The countdown second at which the last-seconds sound window begins. This is also the
     * moment {@link net.onelitefeather.cygnus.phase.LobbyPhase} starts its night transition
     * effect, so the two effects begin in lockstep.
     */
    public static final int WINDOW_START_SECONDS = 10;

    private static final Map<Integer, Sound> COUNTDOWN_SOUNDS = Map.of(
            WINDOW_START_SECONDS, Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.MASTER, 0.5F, 1.000F),
            5, Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.MASTER, 0.6F, 0.944F),
            3, Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.MASTER, 0.7F, 0.667F),
            2, Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.MASTER, 0.75F, 0.630F),
            1, Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.MASTER, 0.85F, 0.561F),
            0, Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.MASTER, 0.9F, 0.500F)
    );

    /**
     * Plays the countdown note assigned to the given tick, if any.
     *
     * @param currentTicks the current countdown value in seconds
     */
    public void onTick(int currentTicks) {
        Sound sound = COUNTDOWN_SOUNDS.get(currentTicks);
        if (sound == null) return;

        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            player.playSound(sound, player.getPosition());
        }
    }

    /**
     * No-op hook kept for structural symmetry with {@link LobbyTimeTransitionTask#reset()}
     * and {@link LobbyWaitingTask}. {@link #onTick(int)} is stateless, so there is nothing
     * to reset — a fresh lobby countdown simply re-enters the same tick values.
     */
    public void reset() {
    }
}
