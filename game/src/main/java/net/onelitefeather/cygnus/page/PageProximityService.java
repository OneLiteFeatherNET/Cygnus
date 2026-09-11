package net.onelitefeather.cygnus.page;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.utils.time.TimeUnit;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.page.PageProximityTarget;
import net.onelitefeather.cygnus.utils.RepeatingTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Plays a chime from every page that is close enough to a survivor to be worth looking for.
 * <p>
 * The chime dynamic is tied to the remaining TTL of the page:
 * <ul>
 *     <li>Phase 1 (TTL &gt; 50%): Silent, survivors must spot the page visually.</li>
 *     <li>Phase 2 (20% &lt; TTL &le; 50%): Warning chime every 3 seconds (60 ticks) with standard pitch.</li>
 *     <li>Phase 3 (TTL &le; 20%): Critical rapid chime every 1 second (20 ticks) with lower pitch and quieter volume.</li>
 * </ul>
 * </p>
 *
 * @author TheMeinerLP
 * @author theEvilReaper
 * @version 1.2.0
 * @since 2.12.0
 */
public final class PageProximityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PageProximityService.class);

    /**
     * The distance in blocks a client carries a sound played at volume 1.
     */
    private static final float VANILLA_SOUND_RANGE = 16.0F;

    public static final double SILENT_TTL_THRESHOLD = 0.50;
    public static final double CRITICAL_TTL_THRESHOLD = 0.20;

    public static final int WARNING_INTERVAL_TICKS = 60;
    public static final int CRITICAL_INTERVAL_TICKS = 20;

    public static final float WARNING_PITCH = 1.0F;
    public static final float CRITICAL_PITCH = 0.75F;
    public static final float CRITICAL_VOLUME_MULTIPLIER = 0.75F;

    public static final int SERVICE_TICK_RATE = 5;

    private final RepeatingTask task = new RepeatingTask(this::tick);
    private final GameConfig config;
    private final Supplier<Collection<Player>> listeners;
    private final Supplier<? extends Collection<? extends PageProximityTarget>> pageSupplier;
    private final Sound warningSound;
    private final Sound criticalSound;
    private final double rangeSquared;
    private final Map<UUID, Long> nextChimeTicks = new HashMap<>();
    private long currentTick;

    /**
     * Creates a new instance of the {@link PageProximityService}.
     *
     * @param config       the configuration holding range and sound
     * @param listeners    supplies the players the hint is played to
     * @param pageSupplier supplies the proximity page targets that can currently be collected
     */
    public PageProximityService(
            GameConfig config,
            Supplier<Collection<Player>> listeners,
            Supplier<? extends Collection<? extends PageProximityTarget>> pageSupplier
    ) {
        this.config = config;
        this.listeners = listeners;
        this.pageSupplier = pageSupplier;
        this.rangeSquared = (double) config.pageProximityRange() * config.pageProximityRange();

        SoundEvent soundEvent = resolveSound(config.pageProximitySound());
        float baseVolume = volumeFor(config.pageProximityRange(), config.pageProximityVolumeFactor());

        this.warningSound = Sound.sound(soundEvent, Sound.Source.MASTER, baseVolume, WARNING_PITCH);
        this.criticalSound = Sound.sound(soundEvent, Sound.Source.MASTER, baseVolume * CRITICAL_VOLUME_MULTIPLIER, CRITICAL_PITCH);
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
        this.task.start(SERVICE_TICK_RATE, TimeUnit.SERVER_TICK);
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
     * Plays the chime from every collectible page that is within range of a listener and eligible to chime.
     */
    void tick() {
        if (!this.config.pageProximityEnabled()) {
            return;
        }

        Collection<? extends PageProximityTarget> pages = this.pageSupplier.get();
        if (pages.isEmpty()) {
            this.nextChimeTicks.clear();
            return;
        }

        this.currentTick += SERVICE_TICK_RATE;

        Set<UUID> currentIds = new HashSet<>();
        Collection<Player> currentListeners = this.listeners.get();

        for (PageProximityTarget page : pages) {
            currentIds.add(page.id());
            double ratio = page.remainingTtlRatio();

            if (ratio > SILENT_TTL_THRESHOLD) {
                // Phase 1: Silent
                continue;
            }

            boolean isCritical = ratio <= CRITICAL_TTL_THRESHOLD;
            int interval = isCritical ? CRITICAL_INTERVAL_TICKS : WARNING_INTERVAL_TICKS;
            Sound soundToPlay = isCritical ? this.criticalSound : this.warningSound;

            long nextChime = this.nextChimeTicks.getOrDefault(page.id(), 0L);
            if (this.currentTick >= nextChime) {
                Pos pagePos = page.position();
                for (Player player : currentListeners) {
                    if (player.getPosition().distanceSquared(pagePos) <= this.rangeSquared) {
                        player.playSound(soundToPlay, pagePos.x(), pagePos.y(), pagePos.z());
                    }
                }
                this.nextChimeTicks.put(page.id(), this.currentTick + interval);
            }
        }

        this.nextChimeTicks.keySet().retainAll(currentIds);
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
