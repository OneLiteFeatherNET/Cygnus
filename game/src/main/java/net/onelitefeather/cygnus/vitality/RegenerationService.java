package net.onelitefeather.cygnus.vitality;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.item.ItemStack;
import net.onelitefeather.cygnus.GameFeature;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.RegenerationConfig;
import net.onelitefeather.cygnus.creek.dread.DreadSource;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.event.PlayerDamagedEvent;
import net.onelitefeather.cygnus.event.SlenderReviveEvent;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.onelitefeather.cygnus.team.TeamHelper;
import net.onelitefeather.cygnus.telemetry.TickSectionNames;
import net.onelitefeather.cygnus.telemetry.TickSections;
import net.onelitefeather.cygnus.utils.Items;
import net.onelitefeather.cygnus.utils.PlayerState;
import net.onelitefeather.cygnus.utils.RepeatingTask;

import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Hands every survivor painkillers that win back some health, once per round.
 * <p>
 * Taking them heals over a few seconds, at most a share of the maximum health that shrinks with
 * the round, and never more than is missing. They can only be taken after
 * {@link RegenerationConfig#delaySeconds()} without a hit. A hit while they work takes its damage off
 * what is still to come, so it counts twice, and the healing goes on with the rest. Fear slows the
 * healing down: a calm survivor regains {@link RegenerationConfig#perSecond()},
 * a terrified one hardly anything.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.17.0
 */
public final class RegenerationService implements GameFeature {

    /** How often a running healing is moved on, in milliseconds. */
    static final long TICK_MILLIS = 100L;

    static final Component STILL_HURTING = Component.text("You have to catch your breath first", NamedTextColor.GRAY, TextDecoration.ITALIC);
    static final Component NOT_HURT = Component.text("You are not hurt", NamedTextColor.GRAY, TextDecoration.ITALIC);

    private final EventNode<Event> node = EventNode.all("regeneration");
    private final RegenerationConfig config;
    private final DreadSource dread;
    private final Supplier<Set<Player>> survivors;
    private final LongSupplier clock;
    private final RepeatingTask task;
    private final PlayerState<Long> lastHit = new PlayerState<>();
    private final PlayerState<Boolean> used = new PlayerState<>();
    private final Map<CygnusPlayer, Double> healing = new ConcurrentHashMap<>();
    private volatile double share;

    /**
     * Creates the service with its tick measured for the slow tick report.
     *
     * @param config    the regeneration settings
     * @param dread     supplies each survivor's fear
     * @param survivors supplies the survivors of the round
     * @param clock     supplies the current time in milliseconds
     * @param sections  measures how long each tick takes
     */
    public RegenerationService(RegenerationConfig config, DreadSource dread, Supplier<Set<Player>> survivors,
                               LongSupplier clock, TickSections sections) {
        this.config = config;
        this.dread = dread;
        this.survivors = survivors;
        this.clock = clock;
        this.task = new RepeatingTask(sections.wrap(TickSectionNames.REGENERATION, this::tick));
        this.registerListeners();
    }

    @Override
    public EventNode<Event> node() {
        return this.node;
    }

    /**
     * Hands out the painkillers at the round's start, follows every hit and forgets the players who
     * are gone.
     */
    private void registerListeners() {
        this.node.addListener(GameStartEvent.class, _ -> this.started());
        this.node.addListener(PlayerUseItemEvent.class, event -> {
            Byte tag = event.getItemStack().getTag(Tags.ITEM_TAG);
            if (tag != null && tag == Items.HEAL_ITEM) this.use(event.getPlayer());
        });
        this.node.addListener(PlayerDamagedEvent.class, this::hit);
        this.node.addListener(PlayerDeathEvent.class, event -> this.forget(event.getPlayer()));
        this.node.addListener(PlayerDisconnectEvent.class, event -> this.forget(event.getPlayer()));
        this.node.addListener(SlenderReviveEvent.class, event -> this.forget(event.getPlayer()));
        this.node.addListener(GameFinishEvent.class, _ -> {
            this.task.stop();
            this.healing.clear();
            this.lastHit.clear();
            this.used.clear();
        });
    }

    /**
     * Remembers the hit and takes its damage off what the painkillers still have to give.
     */
    private void hit(PlayerDamagedEvent event) {
        Player player = event.getPlayer();
        this.lastHit.put(player, this.clock.getAsLong());
        if (!(player instanceof CygnusPlayer survivor)) return;
        double damage = event.getAmount() * Vitality.POINTS_PER_HEALTH;
        this.healing.computeIfPresent(survivor, (_, rest) -> rest > damage ? rest - damage : null);
    }

    /**
     * Fixes the share for the round and gives every survivor their painkillers.
     */
    private void started() {
        this.healing.clear();
        this.lastHit.clear();
        this.used.clear();
        Set<Player> round = this.survivors.get();
        this.share = this.config.shareFor(round.size());
        for (Player survivor : round) {
            Items.setHealItem(survivor);
        }
        this.task.start(TICK_MILLIS, ChronoUnit.MILLIS);
    }

    /**
     * Takes the painkillers, if the survivor may.
     *
     * @param player the player who used them
     * @return {@code true} if they were taken and the healing started
     */
    boolean use(Player player) {
        if (!(player instanceof CygnusPlayer survivor) || !affected(survivor)) return false;
        if (this.used.get(survivor) != null) return false;
        Long last = this.lastHit.get(survivor);
        if (last != null && this.clock.getAsLong() - last < this.config.delaySeconds() * 1000L) {
            survivor.sendActionBar(STILL_HURTING);
            return false;
        }
        Vitality vitality = survivor.vitality();
        double missing = vitality.max() - vitality.points();
        if (missing <= 0.0D) {
            survivor.sendActionBar(NOT_HURT);
            return false;
        }
        this.used.put(survivor, true);
        survivor.getInventory().setItemStack(Items.HEAL_SLOT, ItemStack.AIR);
        this.healing.put(survivor, Math.min(missing, this.share * vitality.max()));
        return true;
    }

    /**
     * Moves every running healing on by one step.
     */
    void tick() {
        for (Map.Entry<CygnusPlayer, Double> entry : Map.copyOf(this.healing).entrySet()) {
            CygnusPlayer survivor = entry.getKey();
            if (!affected(survivor)) {
                this.healing.remove(survivor);
                continue;
            }
            double calm = 1.0D - Math.clamp(this.dread.dreadOf(survivor.getUuid()), 0.0D, 1.0D);
            double step = Math.min(entry.getValue(), this.config.perSecond() * calm * TICK_MILLIS / 1000.0D);
            double rest = entry.getValue() - survivor.vitality().heal(step);
            // Done, or full already with nothing more to give
            if (rest <= 0.0D || survivor.vitality().points() >= survivor.vitality().max()) {
                this.healing.remove(survivor);
            } else {
                this.healing.put(survivor, rest);
            }
        }
    }

    private void forget(Player player) {
        this.healing.remove(player);
        this.lastHit.remove(player);
    }

    /**
     * Tells whether a player may heal: a living survivor who can be hurt.
     */
    private static boolean affected(Player survivor) {
        return TeamHelper.isSurvivorTeam(survivor)
                && !survivor.getGameMode().invulnerable()
                && !survivor.isDead()
                && survivor.getHealth() > 0;
    }
}
