package net.onelitefeather.cygnus.utils;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ViewRuleUpdater {

    public static boolean isViewAble(@NotNull Player player) {
        return player.hasTag(Tags.HIDDEN) && player.getTag(Tags.HIDDEN) == (byte) 0;
    }
}
