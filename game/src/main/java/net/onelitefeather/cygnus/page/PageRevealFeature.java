package net.onelitefeather.cygnus.page;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.network.packet.server.play.SetCooldownPacket;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.onelitefeather.cygnus.GameFeature;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.page.PageProximityTarget;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.event.SlenderReviveEvent;
import net.onelitefeather.cygnus.stamina.FoodBar;
import net.onelitefeather.cygnus.team.TeamHelper;
import net.onelitefeather.cygnus.telemetry.TickSectionNames;
import net.onelitefeather.cygnus.telemetry.TickSections;
import net.onelitefeather.cygnus.utils.Items;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/**
 * The survivors' item that shows the pages around them on their locator bar for a few seconds.
 * <p>
 * Only pages that can be collected right now and lie within the radius are shown, and only to the
 * survivor who used the item. Each waypoint lies a few blocks off its page, at least half the
 * spread, so the bar gives the direction and the last steps still have to be searched. The offset
 * is rolled once per use, so the waypoint does not jump around. A page found meanwhile leaves the
 * bar at once, and a page that moved takes its waypoint along. A use that finds no page costs no
 * cooldown, it only tells the survivor so. The values are fixed on purpose, they are not part of
 * the config.
 * </p>
 * <p>
 * The locator bar shares its place with the experience bar, which carries the stamina. The client
 * shows the experience bar for a few seconds after each change of it, so the pages would vanish
 * while the stamina regenerates. The stamina bar therefore holds its display while the pages are
 * shown. A sprint drains the bar, so it cannot be used while sprinting and a sprint takes the pages
 * off the bar again. A use while sprinting costs no cooldown either.
 * </p>
 *
 * @author Joltra
 * @version 1.1.0
 * @since 2.22.0
 */
public final class PageRevealFeature implements GameFeature {

    /** How far around the survivor pages are found, in blocks. */
    static final int RADIUS = 40;

    /** How long the found pages stay on the locator bar. */
    static final int SECONDS = 10;

    /** The farthest a waypoint lies from its page, in blocks. The nearest is half of it. */
    static final double SPREAD_BLOCKS = 6.0D;

    /** How long a survivor waits after a use that found a page. */
    static final int COOLDOWN_SECONDS = 60;

    /** How often the shown pages are checked, in ticks. */
    static final int RESEND_TICKS = 5;

    /** The client files the cooldown under the item's key, it has no cooldown component. */
    static final String COOLDOWN_GROUP = Items.PAGE_REVEAL_MATERIAL.key().asString();

    static final Component NOTHING_FOUND = Component.text("No pages nearby", NamedTextColor.GRAY, TextDecoration.ITALIC);

    static final Component SPRINTING = Component.text("Stop sprinting first", NamedTextColor.GRAY, TextDecoration.ITALIC);

    private static final Sound DENIED = Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_BASS, Sound.Source.MASTER, 0.6F, 0.5F);
    private static final Sound REVEALED = Sound.sound(SoundEvent.BLOCK_AMETHYST_BLOCK_CHIME, Sound.Source.MASTER, 1.0F, 1.2F);
    private static final double TICK_MILLIS = 50.0D;
    private static final int TICKS_PER_SECOND = 20;

    private final EventNode<Event> node = EventNode.all("page-reveal");
    private final Supplier<? extends Collection<? extends PageProximityTarget>> pages;
    private final Supplier<Set<Player>> survivors;
    private final Function<Player, @Nullable FoodBar> staminaBars;
    private final LongSupplier clock;
    private final TickSections sections;
    private final RandomGenerator random;
    private final Map<UUID, Long> readyAt = new ConcurrentHashMap<>();
    private final Map<Player, Reveal> shown = new ConcurrentHashMap<>();
    private volatile boolean running;
    private @Nullable Task task;

    /**
     * Creates the feature with its updates measured for the slow tick report.
     *
     * @param pages     supplies the pages that can be collected right now
     * @param survivors   supplies the survivors of the round
     * @param staminaBars finds a survivor's stamina bar, whose display is held while the pages are shown
     * @param clock       supplies the current time in milliseconds
     * @param sections    measures how long the updates take
     */
    public PageRevealFeature(Supplier<? extends Collection<? extends PageProximityTarget>> pages,
                             Supplier<Set<Player>> survivors, Function<Player, @Nullable FoodBar> staminaBars,
                             LongSupplier clock, TickSections sections) {
        this(pages, survivors, staminaBars, clock, sections, new SplittableRandom());
    }

    /**
     * Creates the feature with a given random source, for tests.
     *
     * @param pages     supplies the pages that can be collected right now
     * @param survivors   supplies the survivors of the round
     * @param staminaBars finds a survivor's stamina bar, whose display is held while the pages are shown
     * @param clock       supplies the current time in milliseconds
     * @param sections    measures how long the updates take
     * @param random      rolls the offsets of the waypoints
     */
    PageRevealFeature(Supplier<? extends Collection<? extends PageProximityTarget>> pages,
                      Supplier<Set<Player>> survivors, Function<Player, @Nullable FoodBar> staminaBars,
                      LongSupplier clock, TickSections sections, RandomGenerator random) {
        this.pages = pages;
        this.survivors = survivors;
        this.staminaBars = staminaBars;
        this.clock = clock;
        this.sections = sections;
        this.random = random;
        this.registerListeners();
    }

    @Override
    public EventNode<Event> node() {
        return this.node;
    }

    private void registerListeners() {
        this.node.addListener(GameStartEvent.class, _ -> this.started());
        this.node.addListener(PlayerUseItemEvent.class, this::used);
        this.node.addListener(PlayerDeathEvent.class, event -> this.hide(event.getPlayer()));
        this.node.addListener(PlayerDisconnectEvent.class, event -> this.hide(event.getPlayer()));
        this.node.addListener(SlenderReviveEvent.class, event -> this.hide(event.getPlayer()));
        this.node.addListener(GameFinishEvent.class, _ -> this.stop());
    }

    /**
     * Gives every survivor the item, with no cooldown left from an earlier round.
     */
    private void started() {
        this.running = true;
        this.readyAt.clear();
        for (Player survivor : this.survivors.get()) {
            Items.setPageRevealItem(survivor);
        }
        if (this.task == null) {
            this.task = MinecraftServer.getSchedulerManager()
                    .buildTask(this.sections.wrap(TickSectionNames.PAGE_REVEAL, this::step))
                    .delay(TaskSchedule.tick(RESEND_TICKS))
                    .repeat(TaskSchedule.tick(RESEND_TICKS))
                    .schedule();
        }
    }

    private void used(PlayerUseItemEvent event) {
        if (!this.running) return;
        Byte tag = event.getItemStack().getTag(Tags.ITEM_TAG);
        if (tag == null || tag != Items.PAGE_REVEAL_ITEM) return;
        Player player = event.getPlayer();
        if (!TeamHelper.isSurvivorTeam(player)) return;
        this.use(player);
    }

    /**
     * Shows the survivor the pages around him, if the cooldown is over.
     *
     * @param survivor the survivor using the item
     */
    private void use(Player survivor) {
        long now = this.clock.getAsLong();
        if (now < this.readyAt.getOrDefault(survivor.getUuid(), 0L)) {
            survivor.playSound(DENIED, Sound.Emitter.self());
            return;
        }
        if (survivor.isSprinting()) {
            survivor.sendActionBar(SPRINTING);
            return;
        }
        Pos center = survivor.getPosition();
        double rangeSquared = (double) RADIUS * RADIUS;
        List<PageProximityTarget> found = this.pages.get().stream()
                .filter(page -> page.position().distanceSquared(center) <= rangeSquared)
                .map(PageProximityTarget.class::cast)
                .toList();
        if (found.isEmpty()) {
            survivor.sendActionBar(NOTHING_FOUND);
            return;
        }

        this.hide(survivor);
        Reveal reveal = new Reveal(survivor, this.staminaBars.apply(survivor), SECONDS * TICKS_PER_SECOND);
        for (PageProximityTarget page : found) {
            Shown shown = new Shown(UUID.randomUUID(), this.offset());
            shown.target = page.position().add(shown.offset);
            reveal.pages.put(page.id(), shown);
            survivor.sendPacket(PageWaypoint.track(shown.id, shown.target));
        }
        this.shown.put(survivor, reveal);
        if (reveal.staminaBar != null) reveal.staminaBar.holdDisplay();
        survivor.playSound(REVEALED, Sound.Emitter.self());

        long cooldown = COOLDOWN_SECONDS * 1000L;
        this.readyAt.put(survivor.getUuid(), now + cooldown);
        survivor.sendPacket(new SetCooldownPacket(COOLDOWN_GROUP, (int) Math.ceil(cooldown / TICK_MILLIS)));
    }

    /**
     * Rolls how far off its page a waypoint lies: a random direction on the ground, between half
     * the spread and the full spread away, so it never points at the page itself.
     *
     * @return the offset from the page
     */
    private Vec offset() {
        double angle = this.random.nextDouble() * 2.0D * Math.PI;
        double distance = SPREAD_BLOCKS * (0.5D + 0.5D * this.random.nextDouble());
        return new Vec(Math.cos(angle) * distance, 0.0D, Math.sin(angle) * distance);
    }

    /**
     * Counts the shown pages down, takes found pages off the bar and moves the waypoints of pages
     * that moved. A survivor who sprints loses all pages at once.
     */
    private void step() {
        if (this.shown.isEmpty()) return;
        Map<UUID, Pos> current = new HashMap<>();
        for (PageProximityTarget page : this.pages.get()) {
            current.put(page.id(), page.position());
        }
        for (Reveal reveal : List.copyOf(this.shown.values())) {
            reveal.ticksLeft -= RESEND_TICKS;
            if (reveal.ticksLeft <= 0 || !reveal.survivor.isOnline() || reveal.survivor.isSprinting()) {
                this.hide(reveal.survivor);
                continue;
            }
            this.refresh(reveal, current);
        }
    }

    private void refresh(Reveal reveal, Map<UUID, Pos> current) {
        for (Map.Entry<UUID, Shown> entry : List.copyOf(reveal.pages.entrySet())) {
            Shown shown = entry.getValue();
            Pos position = current.get(entry.getKey());
            if (position == null) {
                reveal.pages.remove(entry.getKey());
                reveal.survivor.sendPacket(PageWaypoint.untrack(shown.id));
                continue;
            }
            Pos target = position.add(shown.offset);
            if (target.sameBlock(shown.target)) continue;
            shown.target = target;
            reveal.survivor.sendPacket(PageWaypoint.update(shown.id, target));
        }
        if (reveal.pages.isEmpty() && this.shown.remove(reveal.survivor, reveal)) reveal.release();
    }

    /**
     * Takes every page off the player's locator bar and lets the stamina show again.
     *
     * @param player the player
     */
    private void hide(Player player) {
        Reveal reveal = this.shown.remove(player);
        if (reveal == null) return;
        reveal.release();
        if (!player.isOnline()) return;
        for (Shown shown : reveal.pages.values()) {
            player.sendPacket(PageWaypoint.untrack(shown.id));
        }
    }

    private void stop() {
        this.running = false;
        for (Player player : List.copyOf(this.shown.keySet())) {
            this.hide(player);
        }
        this.readyAt.clear();
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    private static final class Reveal {

        private final Player survivor;
        private final @Nullable FoodBar staminaBar;
        private final Map<UUID, Shown> pages = new ConcurrentHashMap<>();
        private int ticksLeft;

        private Reveal(Player survivor, @Nullable FoodBar staminaBar, int ticksLeft) {
            this.survivor = survivor;
            this.staminaBar = staminaBar;
            this.ticksLeft = ticksLeft;
        }

        private void release() {
            if (this.staminaBar != null) this.staminaBar.releaseDisplay();
        }
    }

    private static final class Shown {

        private final UUID id;
        private final Vec offset;
        private Pos target = Pos.ZERO;

        private Shown(UUID id, Vec offset) {
            this.id = id;
            this.offset = offset;
        }
    }
}
