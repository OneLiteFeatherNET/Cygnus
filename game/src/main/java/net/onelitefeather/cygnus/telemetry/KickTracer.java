package net.onelitefeather.cygnus.telemetry;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.UUID;

/**
 * Creates the span of a player being kicked.
 * <p>
 * A kick of a player carrying the ResourcePack is not instant: the disconnect waits for the client to
 * confirm it dropped the pack, or for a timeout (see {@code CygnusPlayer#kick}). The span covers that
 * wait, and the attribute {@code cygnus.kick.completed_by} says which of the two ended it - the
 * answer to "do clients take the pack off in time, or do we wait out the timeout every time".
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
public final class KickTracer {

    /**
     * A tracer whose spans go nowhere, for players created without tracing.
     */
    public static final KickTracer NONE = new KickTracer(CygnusTracing.noop());

    private final CygnusTracing tracing;

    /**
     * Creates the tracer.
     *
     * @param tracing where the spans go
     */
    public KickTracer(CygnusTracing tracing) {
        this.tracing = tracing;
    }

    /**
     * Starts the span of a kick. The caller ends it, with {@link #complete(TraceStep, String)}.
     *
     * @param player the kicked player
     * @param reason the message the player is kicked with; recorded as plain text
     * @return the open span
     */
    public TraceStep begin(UUID player, Component reason) {
        return this.tracing.root(CygnusAttributes.SPAN_PLAYER_KICK)
                .set(CygnusAttributes.PLAYER_UUID, player.toString())
                .set(CygnusAttributes.KICK_REASON, PlainTextComponentSerializer.plainText().serialize(reason));
    }

    /**
     * Ends the span of a kick.
     *
     * @param step        the span {@link #begin(UUID, Component)} returned
     * @param completedBy what ended the kick, one of the {@code KICK_BY_} constants of
     *                    {@link CygnusAttributes}
     */
    public void complete(TraceStep step, String completedBy) {
        step.set(CygnusAttributes.KICK_COMPLETED_BY, completedBy).close();
    }
}
