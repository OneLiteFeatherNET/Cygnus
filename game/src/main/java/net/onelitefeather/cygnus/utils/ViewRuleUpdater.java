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
        target.updateViewableRule();
        survivor.getPlayers().forEach(Player::updateViewableRule);
    }

    public static boolean isHidden(Player player) {
        return player.hasTag(Tags.HIDDEN) && player.getTag(Tags.HIDDEN) == SlenderBarHelper.HIDDEN;
    }

    private ViewRuleUpdater() {
    }
}
