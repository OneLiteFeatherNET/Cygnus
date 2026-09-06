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
 * where it is. Minecraft carries a sound {@code 16 * volume} blocks, which is why the volume is
 * derived from the configured range instead of being configured on its own - and why the service
 * still clips on the range itself, since a range below 16 blocks would otherwise stay audible past
 * it.</p>
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
 * @version 1.0.0
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
                volumeFor(config.pageProximityRange()),
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
     * Derives the volume that carries the chime across the whole configured range. Anything below
     * the vanilla range of 16 blocks is played at volume 1 - a lower volume would only make the
     * sound quieter, not shorter-ranged, and the range is enforced by {@link #tick()} anyway.
     *
     * @param range the configured range in blocks
     * @return the volume to play the chime at
     */
    private static float volumeFor(int range) {
        return Math.max(1.0F, range / VANILLA_SOUND_RANGE);
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
