package net.onelitefeather.cygnus.creek.state;

import net.minestom.server.coordinate.Pos;
import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.creek.body.CreekBody;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The creek follows one survivor from a distance. Only that survivor can see it.
 * <p>
 * When the survivor looks at the creek, it waits a moment and then teleports out of view.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class StalkState implements CreekState {

    private final UUID target;
    private final long endsAt;
    private long seenSince = -1L;

    /**
     * Creates the state.
     *
     * @param target the stalked survivor
     * @param endsAt when the stalk ends, in milliseconds
     */
    public StalkState(UUID target, long endsAt) {
        this.target = target;
        this.endsAt = endsAt;
    }

    /**
     * Starts a stalk with a random length between {@code stalkMinSeconds} and {@code stalkMaxSeconds}.
     *
     * @param target the survivor to stalk
     * @param ctx    the current step
     * @return the state
     */
    static StalkState starting(UUID target, CreekContext ctx) {
        CreekConfig config = ctx.config();
        long seconds = ctx.random().nextLong(config.stalkMinSeconds(), config.stalkMaxSeconds() + 1L);
        return new StalkState(target, ctx.now() + seconds * 1000L);
    }

    /**
     * Returns the stalked survivor.
     *
     * @return the survivor's id
     */
    public UUID target() {
        return this.target;
    }

    @Override
    public void enter(CreekContext ctx) {
        CreekBody body = ctx.body();
        body.stop();
        body.setAggressive(false);
        body.setFrozen(false);
        body.showTo(Set.of(this.target));
    }

    @Override
    public CreekState tick(CreekContext ctx) {
        Optional<SurvivorView> found = ctx.survivor(this.target);
        if (found.isEmpty() || ctx.now() >= this.endsAt) return VanishState.after(ctx);

        SurvivorView view = found.get();
        CreekConfig config = ctx.config();
        if (view.dread() >= config.huntThreshold()) return HuntState.starting(this.target, ctx);

        CreekBody body = ctx.body();
        body.lookAt(view.eyes());

        if (view.seesCreek()) {
            if (this.seenSince < 0) this.seenSince = ctx.now();
            if (ctx.now() - this.seenSince >= config.stalkRevealMillis() && relocate(ctx, view)) {
                this.seenSince = -1L;
            }
            return this;
        }
        this.seenSince = -1L;

        double distance = body.position().distance(view.position());
        if (distance < config.stalkMinDistance() || distance > config.stalkMaxDistance()) {
            relocate(ctx, view);
        }
        return this;
    }

    /**
     * Teleports the creek to a new spot at the edge of the target's view.
     *
     * @return {@code false} if no free spot was found in this step
     */
    static boolean relocate(CreekContext ctx, SurvivorView view) {
        CreekConfig config = ctx.config();
        Optional<Pos> spot = ctx.spots().beside(view.position(), config.stalkMinDistance(),
                config.stalkMaxDistance(), config.stalkMinAngle(), config.stalkMaxAngle(),
                List.of(view.eyes()), config.personalSpace(), ctx.random());
        spot.ifPresent(ctx.body()::teleport);
        return spot.isPresent();
    }
}
