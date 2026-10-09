package net.onelitefeather.cygnus.possession;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerInputEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.sound.SoundEvent;
import net.onelitefeather.cygnus.GameFeature;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.PossessionConfig;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.event.SlenderReviveEvent;
import net.onelitefeather.cygnus.stamina.SlenderBarHelper;
import net.onelitefeather.cygnus.team.TeamHelper;
import net.onelitefeather.cygnus.utils.Items;
import net.onelitefeather.cygnus.utils.RepeatingTask;
import net.onelitefeather.cygnus.visibility.VisibilityRules;
import org.jetbrains.annotations.Nullable;

import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Lets the hidden slender look through the eyes of the main creek for a few seconds.
 * <p>
 * Meanwhile his body stands revealed and frozen. Only {@link Tags#HIDDEN} changes, the slender bar
 * keeps its state, so the body does no damage. Survivors around the creek glow for him, and the
 * creek is noticed from farther away. Sneaking, the maximum time or the creek going away ends it,
 * and then the full cooldown runs.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.16.0
 */
public final class PossessionFeature implements GameFeature {

    /** How often a running possession is checked, in milliseconds. */
    static final long TICK_MILLIS = 250L;

    static final Component NOT_HIDDEN = Component.text("You have to be hidden to possess the creek", NamedTextColor.GRAY, TextDecoration.ITALIC);
    static final Component CREEK_AWAY = Component.text("The creek is not around", NamedTextColor.GRAY, TextDecoration.ITALIC);
    static final Component TOO_FAR = Component.text("The creek is too far away", NamedTextColor.GRAY, TextDecoration.ITALIC);
    static final Component OCCUPIED = Component.text("Someone else is in the creek", NamedTextColor.GRAY, TextDecoration.ITALIC);

    private static final Sound DENIED = Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_BASS, Sound.Source.MASTER, 0.6F, 0.5F);

    private final EventNode<Event> node = EventNode.all("possession");
    private final PossessionConfig config;
    private final boolean creekEnabled;
    private final PossessionTarget target;
    private final Supplier<Set<Player>> survivors;
    private final Supplier<@Nullable Player> slender;
    private final LongSupplier clock;
    private final PossessionCooldown cooldown;
    private final PossessionGlow glow = new PossessionGlow();
    private final RepeatingTask task = new RepeatingTask(this::tick);
    private boolean running;
    private @Nullable Possession possession;

    /**
     * Creates the feature.
     *
     * @param config       the possession settings
     * @param creekEnabled whether the creek takes part in the game at all
     * @param target       the creek to possess
     * @param survivors    supplies the survivors of the round
     * @param slender      supplies the slender of the round, or {@code null} while there is none
     * @param clock        supplies the current time in milliseconds
     */
    public PossessionFeature(PossessionConfig config, boolean creekEnabled, PossessionTarget target,
                             Supplier<Set<Player>> survivors, Supplier<@Nullable Player> slender, LongSupplier clock) {
        this.config = config;
        this.creekEnabled = creekEnabled;
        this.target = target;
        this.survivors = survivors;
        this.slender = slender;
        this.clock = clock;
        this.cooldown = new PossessionCooldown(config.cooldownSeconds(), clock);
        this.registerListeners();
    }

    /**
     * The possession only exists together with the creek.
     *
     * @return whether the creek is switched on
     */
    @Override
    public boolean enabled() {
        return this.creekEnabled;
    }

    /**
     * Returns the node the feature listens on.
     *
     * @return the feature's own node
     */
    @Override
    public EventNode<Event> node() {
        return this.node;
    }

    /**
     * Hooks the item, sneaking, the role changes and the round's start and end into the node.
     */
    private void registerListeners() {
        this.node.addListener(GameStartEvent.class, _ -> this.started());
        this.node.addListener(PlayerUseItemEvent.class, this::usedItem);
        this.node.addListener(PlayerInputEvent.class, event -> {
            if (event.hasPressedShiftKey()) this.sneaked(event.getPlayer());
        });
        this.node.addListener(SlenderReviveEvent.class, event -> this.slenderChanged(event.getPlayer()));
        this.node.addListener(PlayerDeathEvent.class, event -> this.sneaked(event.getPlayer()));
        this.node.addListener(PlayerDisconnectEvent.class, event -> this.left(event.getPlayer()));
        this.node.addListener(GameFinishEvent.class, _ -> this.stop());
    }

    /**
     * Starts the round for the possession and hands the slender his item.
     */
    synchronized void started() {
        this.running = true;
        Player current = this.slender.get();
        if (current != null) giveItem(current);
    }

    /**
     * Hands out the item one tick later. The game's own listeners fill the slender's inventory on
     * the same event, and they clear it first.
     *
     * @param player the slender
     */
    private static void giveItem(Player player) {
        MinecraftServer.getSchedulerManager().scheduleNextTick(() -> Items.setPossessionItem(player));
    }

    /**
     * Passes a use of the possession item on to {@link #use(Player)}, every other item is ignored.
     *
     * @param event the item use
     */
    private void usedItem(PlayerUseItemEvent event) {
        Byte tag = event.getItemStack().getTag(Tags.ITEM_TAG);
        if (tag == null || tag != Items.POSSESSION_ITEM) return;
        this.use(event.getPlayer());
    }

    /**
     * Tries to possess the creek.
     *
     * @param player the player who used the item
     * @return {@code true} if the possession started
     */
    synchronized boolean use(Player player) {
        if (!this.running || !TeamHelper.isSlenderTeam(player)) return false;
        Possession current = this.possession;
        if (current != null) {
            if (!current.slender().equals(player)) player.sendActionBar(OCCUPIED);
            return false;
        }
        if (!this.cooldown.isReady(player)) {
            player.playSound(DENIED, Sound.Emitter.self());
            return false;
        }
        if (!VisibilityRules.isHidden(player)) {
            player.sendActionBar(NOT_HIDDEN);
            return false;
        }
        Entity creek = this.target.possessable();
        if (creek == null) {
            player.sendActionBar(CREEK_AWAY);
            return false;
        }
        this.target.possess(player.getUuid(), this.config.sightFactor());
        // The client only knows entities within the entity view distance. Farther away, the camera
        // would stay where it is and the slender would pay for nothing.
        if (!creek.getViewers().contains(player)) {
            this.target.release();
            player.sendActionBar(TOO_FAR);
            return false;
        }
        this.begin(player, creek);
        return true;
    }

    /**
     * Starts the possession of a creek the slender's client already knows: reveals the slender,
     * blocks his SlenderEye, moves his camera to the creek and lets the survivors around it glow.
     *
     * @param player the slender
     * @param creek  the creek's entity
     */
    private void begin(Player player, Entity creek) {
        player.setTag(Tags.POSSESSING, true);
        player.setTag(Tags.HIDDEN, SlenderBarHelper.VISIBLE);
        VisibilityRules.refresh(player);
        player.spectate(creek);
        this.possession = Possession.start(player, this.clock.getAsLong(), this.config.maxSeconds());
        this.glow.update(player, Possession.inRange(creek, this.glowRange(), this.survivors.get()));
        this.task.start(TICK_MILLIS, ChronoUnit.MILLIS);
    }

    /**
     * Checks the running possession: ends it when the time is up or the creek is gone, and moves
     * the glow along with the survivors otherwise.
     */
    synchronized void tick() {
        Possession current = this.possession;
        if (current == null) return;
        Entity creek = this.target.possessable();
        if (creek == null || current.isOver(this.clock.getAsLong())) {
            this.end(current, true);
            return;
        }
        this.glow.update(current.slender(), Possession.inRange(creek, this.glowRange(), this.survivors.get()));
    }

    /**
     * Returns how far around the creek survivors glow, the creek's widened sight range.
     *
     * @return the range in blocks
     */
    private double glowRange() {
        return this.target.sightRange() * this.config.sightFactor();
    }

    /**
     * Ends the possession of this player early, with the full cooldown. Used for sneaking and
     * for a slender who dies.
     *
     * @param player the player
     */
    synchronized void sneaked(Player player) {
        Possession current = this.possession;
        if (current != null && current.slender().equals(player)) this.end(current, true);
    }

    /**
     * Hands the role to a new slender. An old slender who still possesses the creek leaves it.
     *
     * @param newSlender the new slender
     */
    private synchronized void slenderChanged(Player newSlender) {
        Possession current = this.possession;
        if (current != null && !current.slender().equals(newSlender)) this.end(current, true);
        giveItem(newSlender);
    }

    /**
     * Cleans up after a slender who left the game, without packets and without a cooldown.
     *
     * @param player the player who left
     */
    synchronized void left(Player player) {
        Possession current = this.possession;
        if (current == null || !current.slender().equals(player)) return;
        this.possession = null;
        this.task.stop();
        this.target.release();
        this.glow.forget();
        player.removeTag(Tags.POSSESSING);
    }

    /**
     * Ends the round for the possession: a running one ends without a cooldown, and every
     * cooldown is forgotten.
     */
    synchronized void stop() {
        this.running = false;
        Possession current = this.possession;
        if (current != null) this.end(current, false);
        this.cooldown.reset();
    }

    /**
     * Ends the possession for an online slender: camera back, glow off, creek released and the
     * slender hidden again.
     *
     * @param current      the running possession
     * @param withCooldown whether the full cooldown starts
     */
    private void end(Possession current, boolean withCooldown) {
        Player player = current.slender();
        this.possession = null;
        this.task.stop();
        this.target.release();
        player.stopSpectating();
        this.glow.clear(player);
        player.removeTag(Tags.POSSESSING);
        player.setTag(Tags.HIDDEN, SlenderBarHelper.HIDDEN);
        VisibilityRules.refresh(player);
        if (withCooldown) this.cooldown.start(player);
    }

    /**
     * Returns the running possession.
     *
     * @return the possession, or {@code null} while nobody possesses the creek
     */
    synchronized @Nullable Possession possession() {
        return this.possession;
    }

    /**
     * Returns the survivors who glow for the slender right now.
     *
     * @return a copy of the set
     */
    synchronized Set<Player> glowing() {
        return this.glow.glowing();
    }
}
