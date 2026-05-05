package net.onelitefeather.cygnus.listener.game;

import net.minestom.server.MinecraftServer;
import net.minestom.server.utils.PacketSendingUtils;
import net.onelitefeather.cygnus.ambient.AmbientProvider;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.page.PageProvider;
import net.onelitefeather.cygnus.common.util.Helper;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.stamina.StaminaService;
import net.onelitefeather.cygnus.utils.Items;
import net.onelitefeather.cygnus.utils.TeamHelper;
import net.theevilreaper.xerus.api.team.TeamService;

import java.util.function.Consumer;

public final class GameStartListener implements Consumer<GameStartEvent> {

    private final TeamService teamService;
    private final AmbientProvider ambientProvider;
    private final StaminaService staminaService;
    private final PageProvider pageProvider;

    public GameStartListener(TeamService teamService, AmbientProvider ambientProvider, StaminaService staminaService, PageProvider pageProvider) {
        this.teamService = teamService;
        this.ambientProvider = ambientProvider;
        this.staminaService = staminaService;
        this.pageProvider = pageProvider;
    }

    @Override
    public void accept(GameStartEvent event) {
        var slenderPlayer = this.teamService.getTeams().get(Helper.SLENDER_ID).getPlayers().stream().findFirst().get();
        slenderPlayer.setTag(Tags.HIDDEN, (byte) 1);
        slenderPlayer.sendMessage(Messages.SLENDER_JOIN_PART);
        Items.setSlenderEye(slenderPlayer);
        this.staminaService.start();
        this.pageProvider.spawn();
        this.ambientProvider.startTask();
        var message = Messages.getSurvivorJoinMessage(String.valueOf(this.pageProvider.getMaxPageAmount()));
        this.teamService.getTeams().get(Helper.SURVIVOR_ID).getPlayers().forEach(player -> {
            player.sendMessage(message);
            player.setTag(Tags.HIDDEN, (byte) 0);
        });
        TeamHelper.updateTabList(this.teamService);
        PacketSendingUtils.broadcastPlayPacket(slenderPlayer.getMetadataPacket());
        MinecraftServer.getConnectionManager().getOnlinePlayers()
                .stream()
                .filter(p -> !p.equals(slenderPlayer))
                .forEach(slenderPlayer::updateOldViewer);
        PacketSendingUtils.broadcastPlayPacket(slenderPlayer.getMetadataPacket());
    }
}
