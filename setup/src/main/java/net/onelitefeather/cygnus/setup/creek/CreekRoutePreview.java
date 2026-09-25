package net.onelitefeather.cygnus.setup.creek;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.onelitefeather.cygnus.common.creek.CreekRoute;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Shows the creek routes to the player editing them: particles at every point and along the
 * route, the route's name above its start, and a marker on ends that link to another route.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class CreekRoutePreview {

    /** How often the particles are drawn, in ticks. */
    static final int INTERVAL_TICKS = 10;

    /** Distance between two particles on a line, in blocks. */
    static final double LINE_STEP = 0.5D;

    /** How far above the start point the name floats, in blocks. */
    static final double LABEL_HEIGHT = 2.5D;

    private final Player player;
    private final Supplier<List<CreekRoute>> routes;
    private final Supplier<@Nullable String> active;
    private final double linkDistance;
    private final List<Entity> labels = new ArrayList<>();
    private @Nullable Task task;

    /**
     * Creates the preview.
     *
     * @param player       the player who sees it
     * @param routes       supplies the current routes
     * @param active       supplies the name of the route being edited, or {@code null}
     * @param linkDistance how close two ends have to be to count as linked, in blocks
     */
    public CreekRoutePreview(Player player, Supplier<List<CreekRoute>> routes, Supplier<@Nullable String> active,
                             double linkDistance) {
        this.player = player;
        this.routes = routes;
        this.active = active;
        this.linkDistance = linkDistance;
    }

    /**
     * Starts drawing. Does nothing if it is already running.
     */
    public void show() {
        if (this.task != null) return;
        this.refreshLabels();
        this.task = MinecraftServer.getSchedulerManager()
                .buildTask(this::draw)
                .repeat(TaskSchedule.tick(INTERVAL_TICKS))
                .schedule();
    }

    /**
     * Stops drawing and removes the names.
     */
    public void hide() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
        this.labels.forEach(Entity::remove);
        this.labels.clear();
    }

    /**
     * Rebuilds the floating names after routes changed. Does nothing while the preview is hidden.
     */
    public void refreshLabels() {
        this.labels.forEach(Entity::remove);
        this.labels.clear();
        Instance instance = this.player.getInstance();
        if (instance == null) return;
        String activeName = this.active.get();
        for (CreekRoute route : this.routes.get()) {
            if (route.points().isEmpty()) continue;
            Vec start = route.points().getFirst();
            Entity label = new Entity(EntityType.TEXT_DISPLAY);
            TextDisplayMeta meta = (TextDisplayMeta) label.getEntityMeta();
            meta.setText(Component.text(route.name(),
                    route.name().equals(activeName) ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
            meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.CENTER);
            label.setAutoViewable(false);
            label.setInstance(instance, new Pos(start.x(), start.y() + LABEL_HEIGHT, start.z()))
                    .thenRun(() -> label.addViewer(this.player));
            this.labels.add(label);
        }
    }

    private void draw() {
        if (!this.player.isOnline()) {
            this.hide();
            return;
        }
        List<CreekRoute> routes = this.routes.get();
        String activeName = this.active.get();
        for (CreekRoute route : routes) {
            boolean isActive = route.name().equals(activeName);
            Particle pointParticle = isActive ? Particle.FLAME : Particle.END_ROD;
            Particle lineParticle = isActive ? Particle.CRIT : Particle.ELECTRIC_SPARK;
            List<Vec> points = route.points();
            for (int i = 0; i < points.size(); i++) {
                this.particle(pointParticle, points.get(i));
                if (i + 1 < points.size()) {
                    line(points.get(i), points.get(i + 1), LINE_STEP).forEach(spot -> this.particle(lineParticle, spot));
                }
            }
        }
        linkedEnds(routes, this.linkDistance).forEach(end -> this.particle(Particle.HAPPY_VILLAGER, end.add(0, 0.5, 0)));
    }

    private void particle(Particle particle, Vec spot) {
        this.player.sendPacket(new ParticlePacket(particle, spot.x(), spot.y() + 0.2D, spot.z(), 0f, 0f, 0f, 0f, 1));
    }

    /**
     * Returns the spots between two points, one every {@code step} blocks, without the points
     * themselves.
     *
     * @param from the first point
     * @param to   the second point
     * @param step the distance between two spots
     * @return the spots
     */
    static List<Vec> line(Vec from, Vec to, double step) {
        double length = from.distance(to);
        List<Vec> spots = new ArrayList<>();
        for (double travelled = step; travelled < length - 1.0E-9; travelled += step) {
            double share = travelled / length;
            spots.add(from.add(to.sub(from).mul(share)));
        }
        return spots;
    }

    /**
     * Returns the route ends that are at most {@code distance} away from an end of another route.
     *
     * @param routes   the routes
     * @param distance the link distance, in blocks
     * @return the linked ends
     */
    static Set<Vec> linkedEnds(List<CreekRoute> routes, double distance) {
        Set<Vec> linked = new HashSet<>();
        for (int a = 0; a < routes.size(); a++) {
            for (int b = a + 1; b < routes.size(); b++) {
                for (Vec first : ends(routes.get(a))) {
                    for (Vec second : ends(routes.get(b))) {
                        if (first.distance(second) <= distance) {
                            linked.add(first);
                            linked.add(second);
                        }
                    }
                }
            }
        }
        return linked;
    }

    private static List<Vec> ends(CreekRoute route) {
        if (route.points().isEmpty()) return List.of();
        return List.of(route.points().getFirst(), route.points().getLast());
    }
}
