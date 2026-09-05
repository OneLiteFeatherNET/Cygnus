package net.onelitefeather.cygnus.gaze;

import net.minestom.server.entity.Player;

/**
 * Where the gaze level of a survivor goes once it has been worked out.
 *
 * <p>The service used to draw the level itself, as a texture on the player's head. Drawing is the
 * resource pack's job now, so what is left is telling the pack which level to draw - and that has
 * to travel through some channel the client already reads. This interface is that seam: the service
 * decides <em>what</em> the level is, a sink decides <em>how</em> it reaches the client.</p>
 *
 * <p>{@link #level(Player, int)} is called only when a survivor's level actually changes, never on
 * every pass. Every channel that can carry this costs something on the client - a bossbar flag is
 * a packet, a biome is a chunk mesh rebuild - so repeating an unchanged value is waste at best and
 * a stutter at worst.</p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.3
 */
public interface GazeSink {

    /** A sink that drops everything, for tests and for running without a signal channel. */
    GazeSink NONE = new GazeSink() {

        @Override
        public void attach(Player survivor) {
            // Nothing to do here
        }

        @Override
        public void detach(Player survivor) {
            // Nothing to do here
        }

        @Override
        public void level(Player survivor, int level) {
            // Nothing to do here
        }
    };

    /**
     * Starts signalling for a survivor, before any level is known.
     *
     * @param survivor the survivor to start signalling for
     */
    void attach(Player survivor);

    /**
     * Stops signalling for a survivor and takes back whatever was set for them.
     *
     * @param survivor the survivor to stop signalling for
     */
    void detach(Player survivor);

    /**
     * Reports a survivor's new level. Called only when it differs from the last one reported.
     *
     * @param survivor the survivor whose level changed
     * @param level    the new level, or {@link SlenderGaze#NONE} when he is out of sight
     */
    void level(Player survivor, int level);
}
