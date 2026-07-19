package net.onelitefeather.cygnus.utils;

import net.theevilreaper.xerus.api.team.Team;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.stamina.SlenderBarHelper;

public final class ViewRuleUpdater {

    public static boolean isViewAble(Player player) {
        return player.hasTag(Tags.HIDDEN) && player.getTag(Tags.HIDDEN) == SlenderBarHelper.HIDDEN;
    }

    public static void updateViewer(Player target, Team survivor) {
        for (Player onlinePlayer : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (onlinePlayer.getUuid().equals(target.getUuid())) continue;
            onlinePlayer.updateViewableRule();
        }
        survivor.getPlayers().forEach(ViewRuleUpdater::showSlender);
        showSurvivor(target);
        target.updateViewableRule();
        survivor.getPlayers().forEach(Player::updateViewableRule);
    }

    public static void showSurvivor(Player player) {
        if (isViewAble(player)) {
            player.setTag(Tags.HIDDEN, SlenderBarHelper.VISIBLE);
        } else {
            player.setTag(Tags.HIDDEN, SlenderBarHelper.HIDDEN);
        }
    }

    public static void showSlender(Player player) {
        if (!isViewAble(player)) {
            player.setTag(Tags.HIDDEN, SlenderBarHelper.HIDDEN);
        } else {
            player.setTag(Tags.HIDDEN, SlenderBarHelper.VISIBLE);
        }
    }

    public static boolean viewableRuleForSlender(Player player) {
        return player.hasTag(Tags.HIDDEN) && player.getTag(Tags.HIDDEN) == SlenderBarHelper.HIDDEN;
    }

    private ViewRuleUpdater() {
    }
}
