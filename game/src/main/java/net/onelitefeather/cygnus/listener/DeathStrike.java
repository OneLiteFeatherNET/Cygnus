package net.onelitefeather.cygnus.listener;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.instance.Instance;
import net.minestom.server.utils.time.TimeUnit;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Lets lightning strike where a survivor died, with thunder that the whole map hears.
 * <p>
 * The bolt is only for show: Minestom neither hurts anyone with it nor sets anything on fire. The
 * thunder is played at the spot with a volume far above 1, the way vanilla does it, so it carries
 * across the whole map without getting quieter and still comes from the right direction.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.16.0
 */
final class DeathStrike {

    /** How long the bolt stays in the world, in ticks. */
    static final int BOLT_TICKS = 20;

    static final Key THUNDER = Key.key("entity.lightning_bolt.thunder");

    /** How loud the thunder is. Anything this loud is heard everywhere on the map. */
    static final float THUNDER_VOLUME = 10_000.0F;

    /**
     * Strikes the spot with lightning and lets everyone in the instance hear the thunder.
     *
     * @param instance where the survivor died
     * @param where    the spot they died on
     */
    void strike(Instance instance, Pos where) {
        Entity bolt = new Entity(EntityType.LIGHTNING_BOLT);
        bolt.setNoGravity(true);
        bolt.setInstance(instance, where);
        bolt.scheduleRemove(BOLT_TICKS, TimeUnit.SERVER_TICK);

        float pitch = 0.8F + ThreadLocalRandom.current().nextFloat() * 0.2F;
        instance.playSound(Sound.sound(THUNDER, Sound.Source.WEATHER, THUNDER_VOLUME, pitch), where.x(), where.y(), where.z());
    }
}
