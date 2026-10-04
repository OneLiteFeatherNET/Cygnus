package net.onelitefeather.cygnus.telemetry;

import net.onelitefeather.cygnus.creek.dread.CreekWitness;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Passes what the creek does to the survivors on to the real witness and records it as actions.
 * <p>
 * The creek reports through {@link CreekWitness} already, so decorating that one interface traces
 * the creek without a line of tracing inside it. The price is that only the survivor is known here,
 * not the creek's own position: a catch cannot name the distance or line of sight between the two.
 * </p>
 * <p>
 * {@link #stalked(UUID)} is called for every step of a stalk. Tracing each would be per-tick state,
 * so only the first step of a stalk becomes a span; a catch or a new selection starts the next one.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
public final class TracingCreekWitness implements CreekWitness {

    private final CreekWitness delegate;
    private final ActionTracer actions;
    private final Function<UUID, ActionTracer.@Nullable Actor> survivors;
    private final Set<UUID> stalking = ConcurrentHashMap.newKeySet();

    /**
     * Creates the decorator.
     *
     * @param delegate  the witness that still has to hear everything
     * @param actions   records the actions
     * @param survivors resolves a survivor's UUID to who and where they are, or {@code null} if they
     *                  are gone
     */
    public TracingCreekWitness(CreekWitness delegate, ActionTracer actions,
                               Function<UUID, ActionTracer.@Nullable Actor> survivors) {
        this.delegate = delegate;
        this.actions = actions;
        this.survivors = survivors;
    }

    @Override
    public void sighted(UUID survivor) {
        trace(CygnusAttributes.ACTION_CREEK_SIGHTED, survivor);
        this.delegate.sighted(survivor);
    }

    @Override
    public void caught(UUID survivor) {
        this.stalking.remove(survivor);
        trace(CygnusAttributes.ACTION_CREEK_CAUGHT, survivor);
        this.delegate.caught(survivor);
    }

    @Override
    public void stalked(UUID survivor) {
        if (this.stalking.add(survivor)) {
            trace(CygnusAttributes.ACTION_CREEK_STALK, survivor);
        }
        this.delegate.stalked(survivor);
    }

    @Override
    public void selected(UUID survivor) {
        this.stalking.remove(survivor);
        trace(CygnusAttributes.ACTION_CREEK_SELECTED, survivor);
        this.delegate.selected(survivor);
    }

    /**
     * A tracing failure must never reach the creek, which is the part of the game that is being
     * observed.
     */
    private void trace(String action, UUID survivor) {
        try {
            ActionTracer.Actor actor = this.survivors.apply(survivor);
            if (actor != null) {
                this.actions.creek(action, actor);
            }
        } catch (RuntimeException ignored) {
            // observing must not change what happens
        }
    }
}
