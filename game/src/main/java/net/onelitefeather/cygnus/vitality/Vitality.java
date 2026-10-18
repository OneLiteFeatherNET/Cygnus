package net.onelitefeather.cygnus.vitality;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.event.EventDispatcher;
import net.onelitefeather.cygnus.attribute.AttributeHelper;
import net.onelitefeather.cygnus.event.PlayerDamagedEvent;

/**
 * Keeps one player's health as points and shows them as Minecraft health.
 * <p>
 * The points are the truth, the hearts only show them: one heart is 10 points, one Minecraft
 * health point is {@value #POINTS_PER_HEALTH} points. The health is rounded up, so a player with
 * any point left never shows an empty bar. Every hit and every heal goes through here.
 * </p>
 * <p>
 * Until it is set up or first hit, it reads the points from the current health, so code and tests
 * that set health directly keep working. From then on the points are its own, and health must not
 * be set past it any more.
 * </p>
 * <p>
 * Each {@code CygnusPlayer} owns one. All calls come from the tick thread, like the rest of the
 * player's round state.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.17.0
 */
public final class Vitality {

    /** Points per Minecraft health point. */
    public static final double POINTS_PER_HEALTH = 5.0D;

    /** Below this a value counts as 0, so drift from many small hits neither kills late nor shows an extra half heart. */
    private static final double EPSILON = 1.0E-9D;

    private final Player player;
    private boolean tracked;
    private double points;
    private double max;

    /**
     * Creates the vitality of a player.
     *
     * @param player the player it belongs to
     */
    public Vitality(Player player) {
        this.player = player;
    }

    /**
     * Gives the player full points for the round: 100 plus the bonus, shown as hearts.
     *
     * @param bonusPoints the extra points, a multiple of {@value #POINTS_PER_HEALTH}
     */
    public void setUp(int bonusPoints) {
        AttributeHelper.updateHealthScale(this.player, (float) (bonusPoints / POINTS_PER_HEALTH));
        this.max = this.player.getAttributeValue(Attribute.MAX_HEALTH) * POINTS_PER_HEALTH;
        this.points = this.max;
        this.tracked = true;
        this.show();
    }

    /**
     * Returns the points the player has left.
     *
     * @return the points, read from the health while it is not tracked yet
     */
    public double points() {
        return this.tracked ? this.points : this.player.getHealth() * POINTS_PER_HEALTH;
    }

    /**
     * Returns the most points the player can have.
     *
     * @return the maximum, read from the max health while it is not tracked yet
     */
    public double max() {
        return this.tracked ? this.max : this.player.getAttributeValue(Attribute.MAX_HEALTH) * POINTS_PER_HEALTH;
    }

    /**
     * Takes points from the player. At 0 points the player dies.
     * <p>
     * The hit is only announced once the shown health drops. A hit too small to show would
     * otherwise splatter blood and play the hurt sound for nothing the player can see.
     * </p>
     *
     * @param amount how many points to take
     * @param source where the hit came from
     * @return the points actually lost
     */
    public double damage(double amount, Point source) {
        if (amount <= 0.0D) return 0.0D;
        this.track();
        double before = this.points;
        float shownBefore = health(before);
        double after = before - amount;
        if (after < EPSILON) after = 0.0D;
        double lost = before - after;
        if (lost <= 0.0D) return 0.0D;
        this.points = after;
        this.show();
        if (health(after) >= shownBefore) return lost;
        // Setting health never raises Minestom's own damage event, so anything reacting to a hit
        // would otherwise never hear about it.
        EventDispatcher.call(new PlayerDamagedEvent(this.player, source, lost));
        return lost;
    }

    /**
     * Gives a living player points back, up to the maximum.
     *
     * @param amount how many points to give
     * @return the points actually gained
     */
    public double heal(double amount) {
        if (amount <= 0.0D) return 0.0D;
        this.track();
        if (this.points <= 0.0D) return 0.0D;
        double after = Math.min(this.max, this.points + amount);
        double gained = after - this.points;
        if (gained <= 0.0D) return 0.0D;
        this.points = after;
        this.show();
        return gained;
    }

    /**
     * Works out the health that shows the given points.
     *
     * @param points the points
     * @return the health, rounded up to whole health points, {@code 0} only at 0 points
     */
    static float health(double points) {
        if (points <= 0.0D) return 0.0F;
        return (float) Math.max(1.0D, Math.ceil(points / POINTS_PER_HEALTH - EPSILON));
    }

    /**
     * Takes over the current health as points, the first time the points are changed.
     */
    private void track() {
        if (this.tracked) return;
        this.points = this.player.getHealth() * POINTS_PER_HEALTH;
        this.max = this.player.getAttributeValue(Attribute.MAX_HEALTH) * POINTS_PER_HEALTH;
        this.tracked = true;
    }

    /**
     * Shows the points as health, but only when the shown value changes. Setting health always
     * sends the player a packet and their metadata to every viewer, and healing changes the points
     * ten times a second while the hearts move far less often.
     */
    private void show() {
        float shown = health(this.points);
        if (this.player.getHealth() != shown) this.player.setHealth(shown);
    }
}
