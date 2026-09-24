package net.onelitefeather.cygnus.creek.state;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.onelitefeather.cygnus.creek.body.CreekBody;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A body that only records what the states ask of it.
 */
public final class RecordingBody implements CreekBody {

    Pos position;
    @Nullable Pos goal;
    double speed;
    int stops;
    final List<Pos> teleports = new ArrayList<>();
    Set<UUID> viewers = Set.of();
    boolean aggressive;
    boolean frozen;
    @Nullable Pos lookedAt;
    boolean removed;

    RecordingBody(Pos position) {
        this.position = position;
    }

    @Override
    public Pos position() {
        return this.position;
    }

    @Override
    public void moveTo(Pos goal, double speed) {
        this.goal = goal;
        this.speed = speed;
    }

    @Override
    public void stop() {
        this.goal = null;
        this.stops++;
    }

    @Override
    public void teleport(Pos position) {
        this.goal = null;
        this.position = position;
        this.teleports.add(position);
    }

    @Override
    public void lookAt(Pos point) {
        this.lookedAt = point;
    }

    @Override
    public void showTo(Set<UUID> viewers) {
        this.viewers = Set.copyOf(viewers);
    }

    @Override
    public boolean isVisibleTo(UUID viewer) {
        return this.viewers.contains(viewer);
    }

    @Override
    public void setAggressive(boolean aggressive) {
        this.aggressive = aggressive;
    }

    @Override
    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    @Override
    public Entity entity() {
        throw new UnsupportedOperationException("The recording body has no entity");
    }

    @Override
    public void remove() {
        this.removed = true;
    }
}
