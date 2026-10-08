package net.onelitefeather.cygnus.footprint;

import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerChunkLoadEvent;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.onelitefeather.cygnus.GameFeature;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.FootprintConfig;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.event.SlenderReviveEvent;
import net.onelitefeather.cygnus.team.TeamHelper;
import net.onelitefeather.cygnus.utils.Items;

import java.util.SplittableRandom;
import java.util.function.LongSupplier;
import java.util.random.RandomGenerator;

/**
 * Footprints for both sides.
 * <p>
 * The hidden slender now and then leaves a print that only survivors see. Survivors are recorded
 * all the time, and the slender reveals their recent tracks around him with his tracker. Moves only
 * count while a round runs, and the round end takes every print and every track away.
 * </p>
 *
 * @author Joltra
 * @version 1.0.0
 * @since 2.17.0
 */
public final class FootprintFeature implements GameFeature {

    private final EventNode<Event> node = EventNode.all("footprint");
    private final LongSupplier clock;
    private final FootprintSpawner spawner;
    private final SurvivorTrackLog log;
    private final SlenderTrail trail;
    private final TrackingScan scan;
    private volatile boolean running;

    /**
     * Creates the feature.
     *
     * @param config the footprint values
     * @param clock  supplies the current time in milliseconds
     */
    public FootprintFeature(FootprintConfig config, LongSupplier clock) {
        this(config, clock, new SplittableRandom());
    }

    /**
     * Creates the feature with a given random source, for tests.
     *
     * @param config the footprint values
     * @param clock  supplies the current time in milliseconds
     * @param random the source for rolls, delays and facings
     */
    FootprintFeature(FootprintConfig config, LongSupplier clock, RandomGenerator random) {
        this.clock = clock;
        this.spawner = new FootprintSpawner(random, config.fadeShare());
        this.log = new SurvivorTrackLog(config);
        this.trail = new SlenderTrail(config, random, this.spawner);
        this.scan = new TrackingScan(config, this.log, this.spawner, clock);
        this.registerListeners();
    }

    @Override
    public EventNode<Event> node() {
        return this.node;
    }

    private void registerListeners() {
        this.node.addListener(GameStartEvent.class, _ -> this.running = true);
        this.node.addListener(PlayerMoveEvent.class, this::moved);
        this.node.addListener(PlayerUseItemEvent.class, this::used);
        this.node.addListener(PlayerChunkLoadEvent.class,
                event -> this.spawner.resend(event.getPlayer(), event.getChunkX(), event.getChunkZ()));
        this.node.addListener(SlenderReviveEvent.class, event -> this.slenderChanged(event.getPlayer()));
        this.node.addListener(PlayerDeathEvent.class, event -> this.died(event.getPlayer()));
        this.node.addListener(PlayerDisconnectEvent.class, event -> this.left(event.getPlayer()));
        this.node.addListener(GameFinishEvent.class, _ -> this.stop());
    }

    private void moved(PlayerMoveEvent event) {
        if (!this.running) return;
        Player player = event.getPlayer();
        if (TeamHelper.isSlenderTeam(player)) {
            this.trail.moved(player, player.getPosition(), event.getNewPosition());
        } else if (TeamHelper.isSurvivorTeam(player)) {
            this.log.moved(player.getUuid(), player.getPosition(), event.getNewPosition(), this.clock.getAsLong());
        }
    }

    /**
     * Hands the role over to a new slender. His own track from his time as a survivor is not a
     * survivor track anymore, and the prints already lying around have to follow his new role.
     *
     * @param slender the new slender
     */
    private void slenderChanged(Player slender) {
        this.log.clear(slender.getUuid());
        this.scan.showCooldown(slender);
        this.spawner.refreshViewers();
    }

    /**
     * Forgets a dead survivor. The death listener has taken the team away by now, so checking the
     * prints again hides the slender's prints from the new spectator.
     *
     * @param player the player who died
     */
    private void died(Player player) {
        this.log.clear(player.getUuid());
        this.spawner.refreshViewers();
    }

    private void used(PlayerUseItemEvent event) {
        if (!this.running) return;
        Byte tag = event.getItemStack().getTag(Tags.ITEM_TAG);
        if (tag == null || tag != Items.TRACKING_ITEM) return;
        Player player = event.getPlayer();
        if (!TeamHelper.isSlenderTeam(player)) return;
        this.scan.use(player);
    }

    /**
     * Forgets a player who left. When the slender leaves, his prints go with him. The cooldown
     * stays, it belongs to the role and the next slender inherits it.
     *
     * @param player the player who left
     */
    private void left(Player player) {
        this.spawner.forget(player);
        this.log.clear(player.getUuid());
        if (TeamHelper.isSlenderTeam(player)) {
            this.spawner.clear();
            this.trail.reset();
        }
    }

    private void stop() {
        this.running = false;
        this.spawner.clear();
        this.log.clearAll();
        this.trail.reset();
        this.scan.reset();
    }

    FootprintSpawner spawner() {
        return this.spawner;
    }

    SurvivorTrackLog log() {
        return this.log;
    }
}
