package net.onelitefeather.cygnus.listener.game;

import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.utils.PacketSendingUtils;
import net.onelitefeather.cygnus.ambient.AmbientProvider;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.page.PageProvider;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.common.page.event.PageSpawnEvent;
import net.onelitefeather.cygnus.hud.player.PlayerPageComponent;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.onelitefeather.cygnus.stamina.SlenderBarHelper;
import net.onelitefeather.cygnus.stamina.StaminaService;
import net.onelitefeather.cygnus.team.TeamHelper;
import net.onelitefeather.cygnus.utils.Items;
import net.theevilreaper.xerus.api.team.Team;
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
        handleSlenderStart();
        handleSurvivorStart();
        startGlobalMechanics();
    }

    private void handleSlenderStart() {
        Team slenderTeam = this.teamService.getTeam(GameConfig.SLENDER_KEY)
                .orElseThrow(() -> new IllegalStateException("Slender team is missing"));
        Player slenderPlayer = slenderTeam.getPlayers().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Slender team has no assigned player"));
        slenderPlayer.setTag(Tags.HIDDEN, SlenderBarHelper.HIDDEN);
        slenderPlayer.sendMessage(Messages.SLENDER_JOIN_PART);
        Items.setSlenderEye(slenderPlayer);

        PacketSendingUtils.broadcastPlayPacket(slenderPlayer.getMetadataPacket());
        MinecraftServer.getConnectionManager().getOnlinePlayers()
                .stream()
                .filter(p -> !p.equals(slenderPlayer))
                .forEach(slenderPlayer::updateOldViewer);
        PacketSendingUtils.broadcastPlayPacket(slenderPlayer.getMetadataPacket());
    }

    private void handleSurvivorStart() {
        Team survivorTeam = this.teamService.getTeam(GameConfig.SURVIVOR_KEY)
                .orElseThrow(() -> new IllegalStateException("Survivor team is missing"));
        Component message = Messages.getSurvivorJoinMessage(String.valueOf(this.pageProvider.getMaxPageAmount()));
        survivorTeam.getPlayers().forEach(player -> {
            player.sendMessage(message);
            player.setTag(Tags.HIDDEN, SlenderBarHelper.VISIBLE);
            CygnusPlayer cygnusPlayer = (CygnusPlayer) player;
            cygnusPlayer.getHudContainer().register(PlayerPageComponent.class, new PlayerPageComponent(cygnusPlayer));
        });
    }

    private void startGlobalMechanics() {
        this.staminaService.start();
        EventDispatcher.call(new PageSpawnEvent());
        this.ambientProvider.startTask();
        TeamHelper.updateTabList(this.teamService);
    }
}
