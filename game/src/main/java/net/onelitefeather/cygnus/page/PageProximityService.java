package net.onelitefeather.cygnus.page;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.utils.time.TimeUnit;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.utils.RepeatingTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Plays a chime from every page that is close enough to a survivor to be worth looking for.
 *
 * <p>The sound is emitted at the page's own position, so the client places it in 3D: a player hears
 * which direction a page is in and roughly how far off it is, without the server ever telling them
 * where it is.</p>
 *
 * <p>Minecraft carries a sound {@code 16 * volume} blocks and fades it to nothing at that distance,
 * so the volume has to follow the configured range rather than being an independent setting. The
 * service clips on the range itself as well, because a range below 16 blocks would otherwise stay
 * audible past it.</p>
 *
 * <p>That clipping is also what makes the volume free to exceed what the range needs, and
 * {@code pageProximityVolumeFactor} does exactly that. It has to: a volume derived to reach exactly
 * the range puts the chime's own silence at the range's edge, which is where the hint is supposed
 * to start being useful.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * PageProximityService service = new PageProximityService(
 *         config, survivorTeam::getPlayers, pageProvider::interactablePagePositions);
 * service.startTask();
 * // ...
 * service.stopTask();
 * }</pre>
 *
 * @author TheMeinerLP
 * @version 1.1.0
 * @since 2.12.0
 */
public final class PageProximityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PageProximityService.class);

    /**
     * The distance in blocks a client carries a sound played at volume 1.
     */
    private static final float VANILLA_SOUND_RANGE = 16.0F;

    /**
     * The pitch stays fixed: the distance is already carried by the 3D position and the falloff, and
     * a second signal on top of that reads as a different sound rather than as a closer one.
     */
    private static final float PITCH = 1.0F;

    private final RepeatingTask task = new RepeatingTask(this::tick);
    private final GameConfig config;
    private final Supplier<Collection<Player>> listeners;
    private final Supplier<List<Pos>> pagePositions;
    private final Sound sound;
    private final double rangeSquared;

    /**
     * Creates a new instance of the {@link PageProximityService}.
     *
     * @param config        the configuration holding range, interval and sound
     * @param listeners     supplies the players the hint is played to
     * @param pagePositions supplies the positions of the pages that can currently be collected
     */
    public PageProximityService(
            GameConfig config,
            Supplier<Collection<Player>> listeners,
            Supplier<List<Pos>> pagePositions
    ) {
        this.config = config;
        this.listeners = listeners;
        this.pagePositions = pagePositions;
        this.rangeSquared = (double) config.pageProximityRange() * config.pageProximityRange();
        this.sound = Sound.sound(
                resolveSound(config.pageProximitySound()),
                Sound.Source.MASTER,
                volumeFor(config.pageProximityRange(), config.pageProximityVolumeFactor()),
                PITCH
        );
    }

    /**
     * Starts the proximity task. Does nothing if the feature is turned off or the task is already
     * running.
     */
    public void startTask() {
        if (!this.config.pageProximityEnabled()) {
            LOGGER.debug("The page proximity hint is turned off, no task is scheduled");
            return;
        }
        this.task.start(this.config.pageProximityInterval(), TimeUnit.SERVER_TICK);
    }

    /**
     * Stops the proximity task. Does nothing if it is not running.
     */
    public void stopTask() {
        this.task.stop();
    }

    /**
     * @return {@code true} while the proximity task is scheduled
     */
    public boolean isRunning() {
        return this.task.isRunning();
    }

    /**
     * Plays the chime from every collectible page that is within range of a listener.
     */
    void tick() {
        if (!this.config.pageProximityEnabled()) {
            return;
        }

        List<Pos> pages = this.pagePositions.get();
        if (pages.isEmpty()) {
            return;
        }

        for (Player player : this.listeners.get()) {
            Pos playerPosition = player.getPosition();
            for (Pos page : pages) {
                if (playerPosition.distanceSquared(page) <= this.rangeSquared) {
                    player.playSound(this.sound, page.x(), page.y(), page.z());
                }
            }
        }
    }

    /**
     * Derives the volume that carries the chime across the configured range, with the falloff
     * stretched past it.
     * <p>
     * Deriving the volume to reach exactly the range made the chime inaudible at the range's own
     * edge: Minecraft fades a sound to nothing at {@code 16 * volume} blocks, so a page at the far
     * end of the range arrived at zero volume - silent precisely where the hint was meant to start
     * being useful. The factor stretches the falloff beyond the range without widening what a
     * player actually hears, because {@link #tick()} drops pages outside the range before playing
     * anything.
     * </p>
     * <p>
     * Anything below the vanilla range of 16 blocks is still floored at volume 1 before the factor
     * applies - a volume below 1 would only make the sound quieter, not shorter-ranged.
     * </p>
     *
     * @param range  the configured range in blocks
     * @param factor how far past the range to stretch the falloff
     * @return the volume to play the chime at
     */
    private static float volumeFor(int range, float factor) {
        return Math.max(1.0F, range / VANILLA_SOUND_RANGE) * factor;
    }

    /**
     * Resolves the configured key against the sound registry. A key that names no known sound would
     * leave the hint silent, which is worse than ignoring the configuration, so it falls back to
     * {@link GameConfig#DEFAULT_PAGE_PROXIMITY_SOUND}.
     *
     * @param key the configured sound key
     * @return the resolved sound
     */
    private static SoundEvent resolveSound(Key key) {
        SoundEvent soundEvent = SoundEvent.fromKey(key);
        if (soundEvent != null) {
            return soundEvent;
        }
        LOGGER.warn("'{}' names no known sound, falling back to {}", key, GameConfig.DEFAULT_PAGE_PROXIMITY_SOUND);
        return SoundEvent.fromKey(GameConfig.DEFAULT_PAGE_PROXIMITY_SOUND);
    }
}
