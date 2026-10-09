package net.onelitefeather.cygnus.possession;

import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.creek.consequence.GlowReveal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lets the survivors around the possessed creek glow, only in the slender's eyes.
 * <p>
 * The glow is a metadata flag that only the slender gets, see {@link GlowReveal#flagsPacket}. Every
 * update sends it again for everyone in range, since any metadata change of a survivor (sprinting,
 * sneaking) gives the slender the real flags and drops the glow.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.16.0
 */
final class PossessionGlow {

    private final Set<Player> glowing = new HashSet<>();

    /**
     * Lets exactly these survivors glow for the slender.
     *
     * @param slender the slender who sees the glow
     * @param inRange the survivors around the creek right now
     */
    void update(Player slender, Set<Player> inRange) {
        for (Player survivor : List.copyOf(this.glowing)) {
            if (inRange.contains(survivor)) continue;
            this.glowing.remove(survivor);
            if (survivor.isOnline()) slender.sendPacket(GlowReveal.flagsPacket(survivor, false));
        }
        for (Player survivor : inRange) {
            this.glowing.add(survivor);
            slender.sendPacket(GlowReveal.flagsPacket(survivor, true));
        }
    }

    /**
     * Turns every glow off for the slender.
     *
     * @param slender the slender who saw the glow
     */
    void clear(Player slender) {
        for (Player survivor : this.glowing) {
            if (survivor.isOnline()) slender.sendPacket(GlowReveal.flagsPacket(survivor, false));
        }
        this.glowing.clear();
    }

    /**
     * Forgets every glow without sending anything, for a slender who is gone.
     */
    void forget() {
        this.glowing.clear();
    }

    /**
     * Returns the survivors who glow right now.
     *
     * @return a copy of the set
     */
    Set<Player> glowing() {
        return Set.copyOf(this.glowing);
    }
}
