package net.onelitefeather.cygnus.page;

import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.time.TimeUnit;
import net.onelitefeather.cygnus.common.page.PageEntity;
import net.onelitefeather.cygnus.common.page.PageNote;
import net.onelitefeather.cygnus.common.page.PageProvider;
import net.onelitefeather.cygnus.utils.RepeatingTask;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Detects survivors looking at collectible pages and floats the page's handwritten note next to
 * it, for that survivor only.
 *
 * <p>Reading is meant to feel calm, so the note does not follow the gaze cone one to one. It only
 * appears after the survivor held the page in view for {@link #DWELL_TICKS}, so a glance while
 * running past shows nothing. Once up, it stays inside a wider cone and range than the one that
 * opened it, so a slight wobble does not make it flicker, and it lingers for {@link #LINGER_TICKS}
 * after the survivor looked away. A page that expires, moves or is picked up takes its note with it
 * at once.</p>
 *
 * @author theEvilReaper
 * @version 2.0.0
 * @since 2.15.0
 */
public final class PageGazeService {

    /**
     * Maximum distance in blocks at which looking at a page starts showing its note.
     */
    public static final double MAX_GAZE_DISTANCE = 4.0D;

    /**
     * Minimum cosine between the view vector and the page direction to start showing a note (~31°).
     */
    public static final double MIN_GAZE_COSINE = 0.85D;

    /**
     * Maximum distance in blocks at which a note that is already up stays up.
     */
    public static final double STAY_GAZE_DISTANCE = 5.5D;

    /**
     * Minimum cosine at which a note that is already up stays up (~46°).
     */
    public static final double STAY_GAZE_COSINE = 0.70D;

    /**
     * How long, in ticks, a page has to stay in view before its note appears.
     */
    public static final int DWELL_TICKS = 6;

    /**
     * How long, in ticks, a note stays up after its page left the stay cone.
     */
    public static final int LINGER_TICKS = 10;

    private static final int UPDATE_INTERVAL_TICKS = 2;
    private static final double DISTANCE_EPSILON = 1.0E-6D;

    private final RepeatingTask task = new RepeatingTask(this::tick);
    private final Supplier<Collection<Player>> survivorSupplier;
    private final Supplier<Collection<PageEntity>> pageSupplier;
    private final Map<Player, GazeState> states = new ConcurrentHashMap<>();
    private long clock;

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
     * Stops the periodic gaze check task and removes every note at once.
     */
    public void stopTask() {
        this.task.stop();
        this.clearAll();
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
     * Removes the note of the given player at once and forgets their gaze.
     *
     * @param player the player to clear
     */
    public void clearPlayer(Player player) {
        GazeState state = this.states.remove(player);
        if (state != null && state.shown != null) {
            state.shown.removeNow();
        }
    }

    /**
     * Returns whether the player currently has a note up, including while it lingers.
     *
     * @param player the player to check
     * @return {@code true} if a note is shown
     */
    public boolean isGazing(Player player) {
        return this.shownNote(player) != null;
    }

    /**
     * Returns the UUID of the page whose note the player currently has up, or {@code null} if none.
     *
     * @param player the player to check
     * @return the page UUID or {@code null}
     */
    public @Nullable UUID activeGaze(Player player) {
        PageNoteDisplay shown = this.shownNote(player);
        return shown == null ? null : shown.pageId();
    }

    @Nullable PageNoteDisplay shownNote(Player player) {
        GazeState state = this.states.get(player);
        return state == null ? null : state.shown;
    }

    /**
     * Performs a single gaze check for all supplied survivors. Each call counts as
     * {@value #UPDATE_INTERVAL_TICKS} ticks.
     */
    public void tick() {
        this.clock += UPDATE_INTERVAL_TICKS;
        Collection<Player> survivors = this.survivorSupplier.get();
        if (survivors == null || survivors.isEmpty()) {
            this.clearAll();
            return;
        }
        for (Player player : List.copyOf(this.states.keySet())) {
            if (!survivors.contains(player)) {
                this.clearPlayer(player);
            }
        }

        Collection<PageEntity> pages = this.pageSupplier.get();
        Collection<PageEntity> current = pages == null ? List.of() : pages;
        for (Player player : survivors) {
            this.update(player, current);
        }
    }

    private void update(Player player, Collection<PageEntity> pages) {
        GazeState state = this.states.computeIfAbsent(player, ignored -> new GazeState());

        if (state.shown != null) {
            this.checkShown(player, state, pages);
        }

        PageEntity best = bestPage(player, pages);
        UUID bestId = best == null ? null : best.getUuid();
        if (bestId == null || (state.shown != null && bestId.equals(state.shown.pageId()))) {
            state.candidate = null;
        } else {
            if (!bestId.equals(state.candidate)) {
                state.candidate = bestId;
                state.candidateSince = this.clock;
            }
            if (this.clock - state.candidateSince + UPDATE_INTERVAL_TICKS >= DWELL_TICKS) {
                Optional<Component> note = PageNote.worldComponentForItem(best.getPageItem());
                if (note.isPresent()) {
                    if (state.shown != null) {
                        state.shown.removeNow();
                    }
                    state.shown = PageNoteDisplay.spawn(player, best, note.get());
                    state.outOfRangeSince = -1;
                }
                state.candidate = null;
            }
        }

        if (state.shown == null && state.candidate == null) {
            this.states.remove(player);
        }
    }

    private void checkShown(Player player, GazeState state, Collection<PageEntity> pages) {
        PageEntity page = findPage(pages, state.shown.pageId());
        if (page == null
                || !page.isInteractable()
                || !page.getPosition().samePoint(state.shown.pagePosition())) {
            state.shown.removeNow();
            state.shown = null;
            state.outOfRangeSince = -1;
            return;
        }

        if (inView(player, page, STAY_GAZE_COSINE, STAY_GAZE_DISTANCE)) {
            state.outOfRangeSince = -1;
            return;
        }
        if (state.outOfRangeSince < 0) {
            state.outOfRangeSince = this.clock;
        }
        if (this.clock - state.outOfRangeSince >= LINGER_TICKS) {
            state.shown.hide();
            state.shown = null;
            state.outOfRangeSince = -1;
        }
    }

    private void clearAll() {
        for (GazeState state : this.states.values()) {
            if (state.shown != null) {
                state.shown.removeNow();
            }
        }
        this.states.clear();
    }

    private static @Nullable PageEntity findPage(Collection<PageEntity> pages, UUID pageId) {
        for (PageEntity page : pages) {
            if (page.getUuid().equals(pageId)) {
                return page;
            }
        }
        return null;
    }

    /**
     * Returns the page the player looks at most directly within the enter cone, or {@code null}.
     */
    private static @Nullable PageEntity bestPage(Player player, Collection<PageEntity> pages) {
        PageEntity bestPage = null;
        double bestCosine = MIN_GAZE_COSINE;
        double bestDistance = Double.MAX_VALUE;

        for (PageEntity page : pages) {
            if (!page.isInteractable() || page.getInstance() == null
                    || !page.getInstance().equals(player.getInstance())) {
                continue;
            }
            Vec toPage = toPage(player, page);
            double distance = toPage.length();
            if (distance > MAX_GAZE_DISTANCE) {
                continue;
            }
            double cosine = cosine(player, toPage, distance);
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
        return bestPage;
    }

    private static boolean inView(Player player, PageEntity page, double minCosine, double maxDistance) {
        if (page.getInstance() == null || !page.getInstance().equals(player.getInstance())) {
            return false;
        }
        Vec toPage = toPage(player, page);
        double distance = toPage.length();
        return distance <= maxDistance && cosine(player, toPage, distance) >= minCosine;
    }

    private static Vec toPage(Player player, PageEntity page) {
        Pos eye = player.getPosition().add(0, player.getEyeHeight(), 0);
        Pos pagePos = page.getPosition();
        return new Vec(pagePos.x() - eye.x(), pagePos.y() - eye.y(), pagePos.z() - eye.z());
    }

    private static double cosine(Player player, Vec toPage, double distance) {
        if (distance < DISTANCE_EPSILON) {
            return 1.0D;
        }
        return player.getPosition().direction().normalize().dot(toPage.div(distance));
    }

    /**
     * What the service knows about one survivor's gaze. Only touched from the gaze task.
     */
    private static final class GazeState {
        private @Nullable UUID candidate;
        private long candidateSince;
        private @Nullable PageNoteDisplay shown;
        private long outOfRangeSince = -1;
    }
}
