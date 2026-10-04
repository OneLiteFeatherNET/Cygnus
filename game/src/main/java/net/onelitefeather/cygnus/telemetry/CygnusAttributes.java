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
