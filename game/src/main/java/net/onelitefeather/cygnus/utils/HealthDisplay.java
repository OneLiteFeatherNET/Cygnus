package net.onelitefeather.cygnus.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;

/**
 * Turns the health of a player into text, counted in hearts the way the client draws them.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.19.0
 */
public final class HealthDisplay {

    /**
     * Shows the hearts a player has left: two health points per heart.
     *
     * @param player the player
     * @return the line with the hearts left and the most hearts the player can have
     */
    public static Component hearts(Player player) {
        String left = formatHearts(player.getHealth());
        String max = formatHearts((float) player.getAttributeValue(Attribute.MAX_HEALTH));
        return Component.text("❤ " + left + " / " + max, NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Turns health points into hearts, rounded to half hearts.
     *
     * @param health the health points
     * @return the hearts, without a fraction for whole hearts
     */
    public static String formatHearts(float health) {
        int halves = Math.max(0, Math.round(health));
        return halves % 2 == 0 ? String.valueOf(halves / 2) : (halves / 2) + ".5";
    }

    private HealthDisplay() {
        throw new UnsupportedOperationException();
    }
}
