package net.onelitefeather.cygnus.telemetry;

import net.onelitefeather.cygnus.creek.dread.HuntEnd;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Measures how long a survivor is hunted: one span per hunt from its start to its end, and the
 * duration as a metric.
 * <p>
 * The creek reports a hunt by the survivor only, not by itself, so two creeks hunting the same
 * survivor cannot be told apart here. Each start opens its own span and each end closes the
 * <em>oldest</em> open one of that survivor - the count stays right, which creek a span belongs to
 * may not.
 * </p>
 * <p>
 * The outcome comes from {@link HuntEnd}: a catch is {@code caught}, a hunt out of time
 * {@code timeout}, a survivor who left or died {@code gone}, a creek sent away {@code escaped}, and
 * the end of the round {@code round_end}. Hunts still open when the round ends are closed as
 * {@code round_end} before the round span ends. A start while no round runs has no span but is still
 * counted, so the metric does not depend on tracing being on.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
public final class HuntTracer {

    private final RoundTracer rounds;
    private final Clock clock;
    private final Supplier<@Nullable String> mapName;
    private final Function<UUID, ActionTracer.@Nullable Actor> survivors;
    private final HuntMetrics metrics;
    private final Map<UUID, ArrayDeque<Hunt>> open = new HashMap<>();

    /**
     * Creates the tracer.
     *
     * @param rounds    owns the round the spans are children of
     * @param clock     dates the start and the end of a hunt
     * @param mapName   supplies the name of the map being played, or {@code null} while none is loaded
     * @param survivors resolves a survivor's UUID to who and where they are, or {@code null} if gone
     * @param metrics   where the duration of a hunt goes
     */
    public HuntTracer(RoundTracer rounds, Clock clock, Supplier<@Nullable String> mapName,
                      Function<UUID, ActionTracer.@Nullable Actor> survivors, HuntMetrics metrics) {
        this.rounds = rounds;
        this.clock = clock;
        this.mapName = mapName;
        this.survivors = survivors;
        this.metrics = metrics;
        rounds.onRoundEnding(this::endAll);
    }

    /**
     * A creek started to hunt a survivor.
     *
     * @param survivor the survivor
     */
    public synchronized void started(UUID survivor) {
        long now = this.clock.millis();
        String map = this.mapName.get();
        TraceStep step = null;
        try {
            step = this.rounds.actionSpan(CygnusAttributes.ACTION_CREEK_HUNT);
            if (step != null) {
                ActionTracer.Actor actor = this.survivors.apply(survivor);
                step.set(CygnusAttributes.PLAYER_UUID, survivor.toString());
                if (actor != null) {
                    step.set(CygnusAttributes.PLAYER_ROLE, actor.role());
                    ActionTracer.position(step, actor.x(), actor.y(), actor.z());
                }
                if (map != null) {
                    step.set(CygnusAttributes.MAP, map);
                }
            }
        } catch (RuntimeException exception) {
            // observing must not change what happens; the hunt is still counted
            if (step != null) {
                step.fail(exception);
            }
        }
        this.open.computeIfAbsent(survivor, _ -> new ArrayDeque<>()).addLast(new Hunt(step, now, map));
    }

    /**
     * A creek stopped hunting a survivor.
     *
     * @param survivor the survivor
     * @param how      how the hunt ended
     */
    public synchronized void ended(UUID survivor, HuntEnd how) {
        ArrayDeque<Hunt> hunts = this.open.get(survivor);
        if (hunts == null) {
            return;
        }
        Hunt hunt = hunts.pollFirst();
        if (hunts.isEmpty()) {
            this.open.remove(survivor);
        }
        if (hunt != null) {
            finish(hunt, outcome(how));
        }
    }

    /**
     * Closes every hunt still open, because the round is ending.
     */
    private synchronized void endAll() {
        List<Hunt> hunts = new ArrayList<>();
        this.open.values().forEach(hunts::addAll);
        this.open.clear();
        hunts.forEach(hunt -> finish(hunt, CygnusAttributes.HUNT_ROUND_END));
    }

    private void finish(Hunt hunt, String outcome) {
        long millis = Math.max(0L, this.clock.millis() - hunt.startedAt);
        TraceStep step = hunt.step;
        try {
            if (step != null) {
                step.set(CygnusAttributes.HUNT_OUTCOME, outcome);
                step.set(CygnusAttributes.HUNT_DURATION_MS, millis);
            }
            this.metrics.record(outcome, hunt.map, millis);
        } catch (RuntimeException exception) {
            if (step != null) {
                step.fail(exception);
            }
        } finally {
            if (step != null) {
                step.close();
            }
        }
    }

    static String outcome(HuntEnd how) {
        return switch (how) {
            case CAUGHT -> CygnusAttributes.HUNT_CAUGHT;
            case TIMEOUT -> CygnusAttributes.HUNT_TIMEOUT;
            case GONE -> CygnusAttributes.HUNT_GONE;
            case SENT_AWAY -> CygnusAttributes.HUNT_ESCAPED;
            case REMOVED -> CygnusAttributes.HUNT_ROUND_END;
        };
    }

    private record Hunt(@Nullable TraceStep step, long startedAt, @Nullable String map) {
    }
}
