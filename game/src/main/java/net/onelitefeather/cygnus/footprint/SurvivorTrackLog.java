package net.onelitefeather.cygnus.footprint;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.onelitefeather.cygnus.common.config.FootprintConfig;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Records where the survivors walked, without showing anything.
 * <p>
 * Every survivor gets a point every few blocks. Points older than the history are dropped
 * whenever a new one comes in, so no task is needed. Nothing is checked against the ground
 * here, that only happens for the few points a scan actually shows.
 * </p>
 *
 * @author Joltra
 * @version 1.0.0
 * @since 2.16.0
 */
final class SurvivorTrackLog {

    private final double sampleBlocks;
    private final double teleportBlocks;
    private final long historyMillis;
    private final Map<UUID, Track> tracks;

    /**
     * Creates the log.
     *
     * @param config the sample distance, the history length and the teleport limit
     */
    SurvivorTrackLog(FootprintConfig config) {
        this.sampleBlocks = config.survivorSampleBlocks();
        this.teleportBlocks = config.teleportBlocks();
        this.historyMillis = config.survivorHistorySeconds() * 1000L;
        this.tracks = new ConcurrentHashMap<>();
    }

    /**
     * Takes a move of a survivor into account.
     *
     * @param survivor the survivor
     * @param from     where the move started
     * @param to       where the move ended
     * @param now      the current time in milliseconds
     */
    void moved(UUID survivor, Pos from, Pos to, long now) {
        Track track = this.tracks.computeIfAbsent(survivor,
                _ -> new Track(new StepMeter(this.sampleBlocks, this.teleportBlocks)));
        synchronized (track) {
            track.prune(now - this.historyMillis);
            if (track.meter.advance(from, to)) {
                track.points.addLast(new TrackPoint(to, now));
            }
        }
    }

    /**
     * Returns the points of all survivors inside a radius that are not newer than a given time.
     *
     * @param center       the center of the radius
     * @param radius       the radius in blocks
     * @param newestMillis the newest time a point may have
     * @param now          the current time in milliseconds
     * @return the matching points, in no particular order
     */
    List<TrackPoint> pointsNear(Point center, double radius, long newestMillis, long now) {
        double radiusSquared = radius * radius;
        long oldest = now - this.historyMillis;
        List<TrackPoint> found = new ArrayList<>();
        for (Track track : this.tracks.values()) {
            synchronized (track) {
                for (TrackPoint point : track.points) {
                    if (point.timeMillis() < oldest || point.timeMillis() > newestMillis) continue;
                    if (point.position().distanceSquared(center) > radiusSquared) continue;
                    found.add(point);
                }
            }
        }
        return found;
    }

    /**
     * Returns how many points a survivor has right now.
     *
     * @param survivor the survivor
     * @return the number of points
     */
    int size(UUID survivor) {
        Track track = this.tracks.get(survivor);
        if (track == null) return 0;
        synchronized (track) {
            return track.points.size();
        }
    }

    /**
     * Forgets a survivor, for example after they died.
     *
     * @param survivor the survivor
     */
    void clear(UUID survivor) {
        this.tracks.remove(survivor);
    }

    /**
     * Forgets every survivor.
     */
    void clearAll() {
        this.tracks.clear();
    }

    private static final class Track {

        private final StepMeter meter;
        private final Deque<TrackPoint> points = new ArrayDeque<>();

        private Track(StepMeter meter) {
            this.meter = meter;
        }

        private void prune(long oldest) {
            while (!this.points.isEmpty() && this.points.peekFirst().timeMillis() < oldest) {
                this.points.removeFirst();
            }
        }
    }
}
