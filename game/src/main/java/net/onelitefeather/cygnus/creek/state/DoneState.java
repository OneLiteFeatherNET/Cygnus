package net.onelitefeather.cygnus.creek.state;

import net.onelitefeather.cygnus.creek.body.CreekBody;

import java.util.Set;

/**
 * A variant whose stalk or hunt is over. It hides the creek and waits to be cleaned up.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class DoneState implements CreekState {

    /** There is nothing to remember in this state, so one instance serves every variant. */
    public static final DoneState INSTANCE = new DoneState();

    private DoneState() {
    }

    @Override
    public void enter(CreekContext ctx) {
        CreekBody body = ctx.body();
        body.stop();
        body.setAggressive(false);
        body.setFrozen(false);
        body.showTo(Set.of());
    }

    @Override
    public CreekState tick(CreekContext ctx) {
        return this;
    }
}
