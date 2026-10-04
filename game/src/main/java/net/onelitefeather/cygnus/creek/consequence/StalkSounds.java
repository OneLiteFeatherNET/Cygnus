package net.onelitefeather.cygnus.creek.consequence;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;

import java.util.random.RandomGenerator;

/**
 * Lets the survivor the creek stalks hear it, from the direction it stands in.
 * <p>
 * A sound of normal volume carries only about 16 blocks, and the stalking creek is often further
 * away. So the sound does not come from the creek itself but from a point on the way to it, at most
 * {@link #MAX_DISTANCE} away from the survivor. The direction stays true and the volume is up to
 * this class instead of to the distance.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.16.0
 */
public final class StalkSounds {

    /** How far from the survivor the sound comes from at most, in blocks. */
    static final double MAX_DISTANCE = 8.0D;

    /** How loud it is at the start of a stalk. */
    static final float FIRST_VOLUME = 0.4F;

    /** How loud it is at the end of a stalk. */
    static final float LAST_VOLUME = 1.0F;

    private static final Key[] SOUNDS = {Key.key("entity.creaking.ambient"), Key.key("entity.creaking.step")};

    private final RandomGenerator random;

    /**
     * Sets up the sounds for a round.
     *
     * @param random picks which sound plays
     */
    public StalkSounds(RandomGenerator random) {
        this.random = random;
    }

    /**
     * Plays one of the creek's sounds to the survivor, from the creek's direction. Nobody else
     * hears it.
     *
     * @param target   the survivor it stalks
     * @param creek    where the creek stands
     * @param progress how far into the stalk it is, from 0 to 1
     */
    public void play(Player target, Pos creek, double progress) {
        Pos from = target.getPosition();
        Vec towards = creek.sub(from).asVec();
        Pos origin = towards.length() <= MAX_DISTANCE ? creek : from.add(towards.normalize().mul(MAX_DISTANCE));
        float volume = (float) (FIRST_VOLUME + (LAST_VOLUME - FIRST_VOLUME) * progress);
        Key sound = SOUNDS[this.random.nextInt(SOUNDS.length)];
        target.playSound(Sound.sound(sound, Sound.Source.HOSTILE, volume, 1.0F), origin.x(), origin.y(), origin.z());
    }
}
