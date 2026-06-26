package net.onelitefeather.cygnus.ambient;

import net.theevilreaper.xerus.api.team.Team;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.potion.TimedPotion;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.Task;
import net.onelitefeather.cygnus.common.Messages;
import org.jetbrains.annotations.Nullable;

import java.time.temporal.ChronoUnit;
import java.util.List;

import static net.onelitefeather.cygnus.common.util.Helper.getRandomPitchValue;

/**
 * Provides ambient effects for a {@link Team} during a game phase.
 *
 * <p>The provider runs a repeating task that plays cave sounds at fixed tick
 * intervals and applies a blindness effect with additional sounds at tick 77,
 * simulating a "lights out" scenario. One full cycle spans {@value #CYCLE_LENGTH} ticks.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * AmbientProvider provider = new AmbientProvider();
 * provider.setTeam(team);
 * provider.startTask();
 * // ...
 * provider.stopTask();
 * }</pre>
 *
 * @author theEvilReaper
 * @version 2.0.0
 * @since 1.0.0
 */
public final class AmbientProvider {

    private static final int CYCLE_LENGTH = 82;
    private static final TimedPotion POTION_EFFECT =
            new TimedPotion(new Potion(PotionEffect.BLINDNESS, (byte) 1, 200), 200);
    private static final Sound[] SOUNDS = {
            Sound.sound(SoundEvent.AMBIENT_CAVE, Sound.Source.MASTER, 1F, 1F),
            Sound.sound(SoundEvent.ENTITY_GENERIC_EXPLODE, Sound.Source.MASTER, 1F, 0F),
            Sound.sound(SoundEvent.BLOCK_FIRE_EXTINGUISH, Sound.Source.MASTER, 1F, 1F),
            Sound.sound(SoundEvent.ENTITY_WITHER_SPAWN, Sound.Source.MASTER, 5F, 0.7F)
    };

    private final Team team;
    private @Nullable Task task;
    private int currentTicks;

    /**
     * Creates a new instance of the {@link AmbientProvider}.
     * @param team which is involved
     */
    public AmbientProvider(Team team) {
        this.team = team;
    }

    /**
     * Starts the ambient task.
     */
    public void startTask() {
        if (task != null) return;
        task = MinecraftServer.getSchedulerManager()
                .buildTask(this::tick)
                .repeat(1, ChronoUnit.SECONDS)
                .schedule();
    }

    /**
     * Stops the ambient task.
     */
    public void stopTask() {
        if (task == null) return;
        task.cancel();
        task = null;
    }

    /**
     * Executes the ambient logic.
     */
    public void tick() {
        List<Player> players = List.copyOf(this.team.getPlayers());

        switch (currentTicks) {
            case 0, 15, 30, 45, 60 -> {
                Sound caveSound = Sound.sound(SoundEvent.AMBIENT_CAVE,
                        Sound.Source.MASTER, 1F, getRandomPitchValue());
                for (Player player : players) {
                    player.playSound(caveSound, player.getPosition());
                }
            }
            case 77 -> {
                for (Player player : players) {
                    player.addEffect(POTION_EFFECT.potion());
                    player.playSound(SOUNDS[1], player.getPosition());
                    player.playSound(SOUNDS[2], player.getPosition());
                    player.playSound(SOUNDS[3], player.getPosition());
                    player.sendMessage(Messages.LIGHT_WENT_OUT);
                }
            }
            default -> {
                // Nothing to do here
            }
        }

        currentTicks = (currentTicks + 1) % CYCLE_LENGTH;
    }
}
