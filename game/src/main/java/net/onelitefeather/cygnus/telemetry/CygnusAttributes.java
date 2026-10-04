package net.onelitefeather.cygnus.telemetry;

import io.opentelemetry.api.common.AttributeKey;

/**
 * The span names, event names and attribute keys Cygnus uses, in one place.
 * <p>
 * Everything custom lives under the {@code cygnus.} namespace so it cannot collide with a
 * semantic convention. A player is identified by the UUID only: the deprecated {@code enduser.id}
 * is not used, and no address of a player is ever recorded.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
public final class CygnusAttributes {

    /**
     * The name the instrumentation scope is registered under.
     */
    public static final String INSTRUMENTATION_NAME = "net.onelitefeather.cygnus";

    // --- Spans ---------------------------------------------------------------------------------

    /** The whole start of the service, from the composition root until the server is wired up. */
    public static final String SPAN_STARTUP = "cygnus.startup";
    /** The stop of the server up to the moment the JVM is told to exit. */
    public static final String SPAN_SHUTDOWN = "cygnus.shutdown";
    /** One round, from the lobby opening to the restart ending. */
    public static final String SPAN_ROUND = "cygnus.round";
    /** Prefix of the phase spans; the lower-cased phase name is appended. */
    public static final String SPAN_PHASE_PREFIX = "cygnus.phase.";
    /** A player joining, from the configuration phase to the first spawn. */
    public static final String SPAN_PLAYER_JOIN = "cygnus.player.join";
    /** A player being kicked, including the wait for the client to drop the ResourcePack. */
    public static final String SPAN_PLAYER_KICK = "cygnus.player.kick";
    /** A server tick that took longer than the configured threshold. */
    public static final String SPAN_SLOW_TICK = "cygnus.tick.slow";
    /** The share one service had in a slow tick. */
    public static final String SPAN_TICK_SECTION = "cygnus.tick.section";

    // --- Action spans (children of the round span) ---------------------------------------------

    public static final String ACTION_PAGE_SPAWN = "cygnus.action.page.spawn";
    public static final String ACTION_PAGE_FOUND = "cygnus.action.page.found";
    public static final String ACTION_PAGE_EXPIRED = "cygnus.action.page.expired";
    public static final String ACTION_PLAYER_DEATH = "cygnus.action.player.death";
    public static final String ACTION_SLENDER_REVIVE = "cygnus.action.slender.revive";
    public static final String ACTION_SLENDER_STAMINA = "cygnus.action.slender.stamina";
    public static final String ACTION_SPECTATOR_JOIN = "cygnus.action.spectator.join";
    public static final String ACTION_DISCLAIMER_ACKNOWLEDGE = "cygnus.action.disclaimer.acknowledge";
    public static final String ACTION_DISCLAIMER_DECLINE = "cygnus.action.disclaimer.decline";
    public static final String ACTION_BLACKOUT = "cygnus.action.blackout";
    public static final String ACTION_BLACKOUT_PLAYER = "cygnus.action.blackout.player";
    public static final String ACTION_SANITY_THRESHOLD = "cygnus.action.sanity.threshold";
    public static final String ACTION_CREEK_SIGHTED = "cygnus.action.creek.sighted";
    public static final String ACTION_CREEK_SELECTED = "cygnus.action.creek.selected";
    public static final String ACTION_CREEK_STALK = "cygnus.action.creek.stalk";
    public static final String ACTION_CREEK_CAUGHT = "cygnus.action.creek.caught";

    // --- Events on the round span --------------------------------------------------------------

    public static final String EVENT_GAME_START = "cygnus.game.start";
    public static final String EVENT_GAME_FINISH = "cygnus.game.finish";
    public static final String EVENT_PAGE_FOUND = "cygnus.page.found";
    public static final String EVENT_PLAYER_DEATH = "cygnus.player.death";
    public static final String EVENT_SLENDER_REVIVE = "cygnus.slender.revive";

    // --- Events on the join span ---------------------------------------------------------------

    public static final String EVENT_JOIN_CONFIGURATION = "cygnus.join.configuration";
    public static final String EVENT_JOIN_RESOURCEPACK = "cygnus.join.resourcepack";

    // --- Attributes ----------------------------------------------------------------------------

    public static final AttributeKey<String> ROUND_ID = AttributeKey.stringKey("cygnus.round.id");
    public static final AttributeKey<String> PHASE_NAME = AttributeKey.stringKey("cygnus.phase.name");
    public static final AttributeKey<String> PLAYER_UUID = AttributeKey.stringKey("cygnus.player.uuid");
    public static final AttributeKey<String> PLAYER_ROLE = AttributeKey.stringKey("cygnus.player.role");
    public static final AttributeKey<String> GAME_END_REASON = AttributeKey.stringKey("cygnus.game.end_reason");
    public static final AttributeKey<Long> PAGES_FOUND = AttributeKey.longKey("cygnus.pages.found");
    public static final AttributeKey<Long> PAGES_MAX = AttributeKey.longKey("cygnus.pages.max");
    public static final AttributeKey<String> JOIN_OUTCOME = AttributeKey.stringKey("cygnus.join.outcome");
    public static final AttributeKey<String> RESOURCEPACK_STATUS = AttributeKey.stringKey("cygnus.resourcepack.status");
    public static final AttributeKey<String> KICK_REASON = AttributeKey.stringKey("cygnus.kick.reason");
    public static final AttributeKey<String> KICK_COMPLETED_BY = AttributeKey.stringKey("cygnus.kick.completed_by");
    public static final AttributeKey<Double> TICK_DURATION_MS = AttributeKey.doubleKey("cygnus.tick.duration_ms");
    public static final AttributeKey<Double> TICK_ACQUISITION_MS = AttributeKey.doubleKey("cygnus.tick.acquisition_ms");
    public static final AttributeKey<Long> TICK_THRESHOLD_MS = AttributeKey.longKey("cygnus.tick.threshold_ms");
    public static final AttributeKey<String> TICK_SECTION_NAME = AttributeKey.stringKey("cygnus.tick.section.name");
    public static final AttributeKey<Double> TICK_SECTION_DURATION_MS = AttributeKey.doubleKey("cygnus.tick.section.duration_ms");

    public static final AttributeKey<String> MAP = AttributeKey.stringKey("cygnus.map");
    public static final AttributeKey<Double> POSITION_X = AttributeKey.doubleKey("cygnus.position.x");
    public static final AttributeKey<Double> POSITION_Y = AttributeKey.doubleKey("cygnus.position.y");
    public static final AttributeKey<Double> POSITION_Z = AttributeKey.doubleKey("cygnus.position.z");
    public static final AttributeKey<String> PAGE_ID = AttributeKey.stringKey("cygnus.page.id");
    public static final AttributeKey<Long> PAGE_INDEX = AttributeKey.longKey("cygnus.page.index");
    public static final AttributeKey<Boolean> PAGE_RELOCATED = AttributeKey.booleanKey("cygnus.page.relocated");
    public static final AttributeKey<Long> PAGE_OUT_MS = AttributeKey.longKey("cygnus.page.out_ms");
    public static final AttributeKey<Double> PAGE_SPOT_X = AttributeKey.doubleKey("cygnus.page.spot.x");
    public static final AttributeKey<Double> PAGE_SPOT_Y = AttributeKey.doubleKey("cygnus.page.spot.y");
    public static final AttributeKey<Double> PAGE_SPOT_Z = AttributeKey.doubleKey("cygnus.page.spot.z");
    public static final AttributeKey<Double> DISTANCE = AttributeKey.doubleKey("cygnus.distance");
    public static final AttributeKey<String> KILLER_UUID = AttributeKey.stringKey("cygnus.killer.uuid");
    public static final AttributeKey<String> STAMINA_STATE = AttributeKey.stringKey("cygnus.stamina.state");
    public static final AttributeKey<Long> LINKS_DROPPED = AttributeKey.longKey("cygnus.links.dropped");
    public static final AttributeKey<String> BLACKOUT_TEAM = AttributeKey.stringKey("cygnus.blackout.team");
    public static final AttributeKey<Long> BLACKOUT_PLAYERS = AttributeKey.longKey("cygnus.blackout.players");
    public static final AttributeKey<Long> BLACKOUT_DURATION_TICKS = AttributeKey.longKey("cygnus.blackout.duration_ticks");
    public static final AttributeKey<Long> BLACKOUT_NEXT_IN_S = AttributeKey.longKey("cygnus.blackout.next_in_s");
    public static final AttributeKey<Double> SANITY_VALUE = AttributeKey.doubleKey("cygnus.sanity.value");
    public static final AttributeKey<Double> SANITY_FEAR = AttributeKey.doubleKey("cygnus.sanity.fear");
    public static final AttributeKey<String> SANITY_BAND = AttributeKey.stringKey("cygnus.sanity.band");
    public static final AttributeKey<String> SANITY_BAND_FROM = AttributeKey.stringKey("cygnus.sanity.band.from");
    public static final AttributeKey<String> SANITY_SOURCE = AttributeKey.stringKey("cygnus.sanity.source");
    public static final AttributeKey<String> LINK_KIND = AttributeKey.stringKey("cygnus.link.kind");

    /** The link points at the round trace a player's cookie remembered. */
    public static final String LINK_PREVIOUS_ROUND = "previous_round";

    // --- Values of kick.completed_by -----------------------------------------------------------

    /** The client confirmed it dropped the pack. */
    public static final String KICK_BY_ACK = "ack";
    /** The client did not answer within the timeout. */
    public static final String KICK_BY_TIMEOUT = "timeout";
    /** The client left on its own before it answered. */
    public static final String KICK_BY_DISCONNECTED = "disconnected";
    /** There was nothing to wait for. */
    public static final String KICK_BY_IMMEDIATE = "immediate";

    private CygnusAttributes() {
    }
}
