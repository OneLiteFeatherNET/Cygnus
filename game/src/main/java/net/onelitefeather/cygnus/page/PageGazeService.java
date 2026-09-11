package net.onelitefeather.cygnus.page;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.time.TimeUnit;
import net.onelitefeather.cygnus.common.page.PageEntity;
import net.onelitefeather.cygnus.common.page.PageNote;
import net.onelitefeather.cygnus.common.page.PageProvider;
import net.onelitefeather.cygnus.utils.RepeatingTask;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Detects survivors looking directly at active collectible pages within first-person view
 * and displays an atmospheric handwritten horror note via a subtitle tooltip.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class PageGazeService {

    /**
     * Maximum distance in blocks at which a survivor can focus on a page.
     */
    public static final double MAX_GAZE_DISTANCE = 4.0D;

    /**
     * Minimum cosine between the player's view vector and the page direction (~31 degree cone).
     */
    public static final double MIN_GAZE_COSINE = 0.85D;

    /**
     * Subtitle title animation timings (100ms fade-in, 5000ms stay, 150ms fade-out).
     */
    public static final Title.Times TITLE_TIMES = Title.Times.times(
            Duration.ofMillis(100),
            Duration.ofSeconds(5),
            Duration.ofMillis(150)
    );

    private static final int UPDATE_INTERVAL_TICKS = 2;
    private static final double DISTANCE_EPSILON = 1.0E-6D;

    private final RepeatingTask task = new RepeatingTask(this::tick);
    private final Supplier<Collection<Player>> survivorSupplier;
    private final Supplier<Collection<PageEntity>> pageSupplier;
    private final Map<Player, UUID> activeGaze = new ConcurrentHashMap<>();

    /**
     * Constructs a new {@link PageGazeService}.
     *
     * @param survivorSupplier supplies the survivors to monitor
     * @param pageSupplier     supplies the collectible pages
     */
    public PageGazeService(
            Supplier<Collection<Player>> survivorSupplier,
            Supplier<Collection<PageEntity>> pageSupplier
    ) {
        this.survivorSupplier = survivorSupplier;
        this.pageSupplier = pageSupplier;
    }

    /**
     * Constructs a new {@link PageGazeService} using a {@link PageProvider}.
     *
     * @param survivorSupplier supplies the survivors to monitor
     * @param pageProvider     the page provider supplying interactable pages
     */
    public PageGazeService(
            Supplier<Collection<Player>> survivorSupplier,
            PageProvider pageProvider
    ) {
        this(survivorSupplier, pageProvider::interactablePages);
    }

    /**
     * Starts the periodic gaze check task. Does nothing if already running.
     */
    public void startTask() {
        this.task.start(UPDATE_INTERVAL_TICKS, TimeUnit.SERVER_TICK);
    }

    /**
     * Stops the periodic gaze check task and clears all active player tooltip titles.
     */
    public void stopTask() {
        this.task.stop();
        for (Player player : this.activeGaze.keySet()) {
            player.clearTitle();
        }
        this.activeGaze.clear();
    }

    /**
     * Returns whether the gaze check task is currently running.
     *
     * @return {@code true} if running
     */
    public boolean isRunning() {
        return this.task.isRunning();
    }

    /**
     * Clears the gaze state and tooltip title for the specified player.
     *
     * @param player the player to clear
     */
    public void clearPlayer(Player player) {
        if (this.activeGaze.remove(player) != null) {
            player.clearTitle();
        }
    }

    /**
     * Returns whether the player is currently gazing at an active page.
     *
     * @param player the player to check
     * @return {@code true} if gazing
     */
    public boolean isGazing(Player player) {
        return this.activeGaze.containsKey(player);
    }

    /**
     * Returns the UUID of the page the player is currently gazing at, or {@code null} if none.
     *
     * @param player the player to check
     * @return the page UUID or {@code null}
     */
    public @Nullable UUID activeGaze(Player player) {
        return this.activeGaze.get(player);
    }

    /**
     * Performs a single tick of gaze detection for all supplied survivors.
     */
    public void tick() {
        Collection<Player> survivors = this.survivorSupplier.get();
        if (survivors == null || survivors.isEmpty()) {
            if (!this.activeGaze.isEmpty()) {
                for (Player player : this.activeGaze.keySet()) {
                    player.clearTitle();
                }
                this.activeGaze.clear();
            }
            return;
        }

        this.activeGaze.keySet().removeIf(player -> {
            if (!survivors.contains(player)) {
                player.clearTitle();
                return true;
            }
            return false;
        });

        Collection<PageEntity> pages = this.pageSupplier.get();
        if (pages == null || pages.isEmpty()) {
            for (Player player : this.activeGaze.keySet()) {
                player.clearTitle();
            }
            this.activeGaze.clear();
            return;
        }

        for (Player player : survivors) {
            updatePlayerGaze(player, pages);
        }
    }

    private void updatePlayerGaze(Player player, Collection<PageEntity> pages) {
        Pos eyePos = player.getPosition().add(0, player.getEyeHeight(), 0);
        Vec direction = player.getPosition().direction().normalize();

        PageEntity bestPage = null;
        double bestCosine = MIN_GAZE_COSINE;
        double bestDistance = Double.MAX_VALUE;

        for (PageEntity page : pages) {
            if (!page.isInteractable()) {
                continue;
            }
            if (page.getInstance() == null || !page.getInstance().equals(player.getInstance())) {
                continue;
            }

            Vec vector = new Vec(
                    page.getPosition().x() - eyePos.x(),
                    page.getPosition().y() - eyePos.y(),
                    page.getPosition().z() - eyePos.z()
            );
            double distance = vector.length();
            if (distance > MAX_GAZE_DISTANCE) {
                continue;
            }

            double cosine;
            if (distance < DISTANCE_EPSILON) {
                cosine = 1.0D;
            } else {
                cosine = direction.dot(vector.div(distance));
            }

            if (cosine < MIN_GAZE_COSINE) {
                continue;
            }

            if (bestPage == null
                    || cosine > bestCosine
                    || (Math.abs(cosine - bestCosine) < DISTANCE_EPSILON && distance < bestDistance)) {
                bestPage = page;
                bestCosine = cosine;
                bestDistance = distance;
            }
        }

        if (bestPage != null) {
            UUID currentUuid = bestPage.getUuid();
            UUID previousUuid = this.activeGaze.get(player);
            if (!currentUuid.equals(previousUuid)) {
                Optional<Component> tooltip = PageNote.tooltipForItem(bestPage.getPageItem());
                if (tooltip.isPresent()) {
                    player.showTitle(Title.title(Component.empty(), tooltip.get(), TITLE_TIMES));
                    this.activeGaze.put(player, currentUuid);
                } else {
                    if (previousUuid != null) {
                        player.clearTitle();
                        this.activeGaze.remove(player);
                    }
                }
            }
        } else {
            UUID removed = this.activeGaze.remove(player);
            if (removed != null) {
                player.clearTitle();
            }
        }
    }
}
