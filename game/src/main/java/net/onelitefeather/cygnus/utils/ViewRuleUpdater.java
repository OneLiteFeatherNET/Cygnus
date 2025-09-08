package net.onelitefeather.cygnus.utils;

import net.theevilreaper.xerus.api.team.Team;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.Tags;
import org.jetbrains.annotations.NotNull;

public final class ViewRuleUpdater {

    public static boolean isViewAble(@NotNull Player player) {
        return player.hasTag(Tags.HIDDEN) && player.getTag(Tags.HIDDEN) == (byte) 1;
    }
    public static void updateViewer(@NotNull Player target, @NotNull Team survivor) {
        for (Player onlinePlayer : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (onlinePlayer.getUuid().equals(target.getUuid())) continue;
            onlinePlayer.updateViewableRule();
        }
        survivor.getPlayers().forEach(ViewRuleUpdater::showSlender);
        showSurvivor(target);
        target.updateViewableRule();
        survivor.getPlayers().forEach(Player::updateViewableRule);
    }

    public static void showSurvivor(@NotNull Player player) {
        if (isViewAble(player)) {
            player.setTag(Tags.HIDDEN, (byte) 0);
        } else {
            player.setTag(Tags.HIDDEN, (byte) 1);
        }
    }

    public static void showSlender(Player player) {
        if (!isViewAble(player)) {
            player.setTag(Tags.HIDDEN, (byte) 1);
        } else {
            player.setTag(Tags.HIDDEN, (byte) 0);
        }
    }

    public static boolean viewableRuleForSlender(@NotNull Player player) {
        return player.hasTag(Tags.HIDDEN) && player.getTag(Tags.HIDDEN) == (byte) 1;
    }

    private ViewRuleUpdater() {
    }
}
