package net.onelitefeather.cygnus.ambient;

import de.icevizion.xerus.api.team.Team;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.potion.TimedPotion;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.Task;
import net.minestom.server.utils.validate.Check;
import net.onelitefeather.cygnus.common.Messages;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.ChronoUnit;

import static net.onelitefeather.cygnus.utils.Helper.getRandomPitchValue;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
@SuppressWarnings({"java:S3252"})
public final class AmbientProvider {

    private static final int MAX_TICKS = 80;
    private Task task;
    private int currentTicks;
    private final TimedPotion potionEffect;
    private final Sound[] sounds;
    private Team team;

    public AmbientProvider() {
        this.potionEffect = new TimedPotion(new Potion(PotionEffect.BLINDNESS, (byte) 1 , 200), 200);
        this.sounds = new Sound[4];
        this.sounds[0] = Sound.sound(SoundEvent.AMBIENT_CAVE, Sound.Source.MASTER, 1F, 1F);
        this.sounds[1] = Sound.sound(SoundEvent.ENTITY_GENERIC_EXPLODE, Sound.Source.MASTER, 1F, 0F);
        this.sounds[2] = Sound.sound(SoundEvent.BLOCK_FIRE_EXTINGUISH, Sound.Source.MASTER, 1F, 1F);
        this.sounds[3] = Sound.sound(SoundEvent.ENTITY_WITHER_SPAWN, Sound.Source.MASTER, 5F, 0.7F);
    }

    // TODO: I think we can use here only the set reference from the team instead of the whole team reference
    public void setTeam(@NotNull Team team) {
        this.team = team;
    }

    public void startTask() {
        if (task != null) return;
        Check.argCondition(team == null, "The team variable can't be null");
        task = MinecraftServer.getSchedulerManager().buildTask(this::tick).repeat(1, ChronoUnit.SECONDS).schedule();
    }

    public void stopTask() {
        if (task == null) return;
        this.task.cancel();
        this.task = null;
    }

    // TODO: The iteration over the players can be result into a race condition because the players from a team can be changed each time
    public void tick() {
        switch (currentTicks) {
            case 0, 15, 30, 45, 60:
                for (Player player : this.team.getPlayers()) {
                    player.playSound(Sound.sound(SoundEvent.AMBIENT_CAVE, Sound.Source.MASTER, 1F, getRandomPitchValue()), player.getPosition());
                }
                break;
            case 77:
                for (Player player : this.team.getPlayers()) {
                    player.addEffect(potionEffect.potion());
                    player.playSound(sounds[1], player.getPosition());
                    player.playSound(sounds[2], player.getPosition());
                    player.playSound(sounds[3], player.getPosition());
                    player.sendMessage(Messages.LIGHT_WENT_OUT);
                }
                break;
            default: {
                //NOTHING TODO here
            }
        }
        if (currentTicks > MAX_TICKS) {
            currentTicks = 0;
        } else {
            currentTicks++;
        }
    }
}