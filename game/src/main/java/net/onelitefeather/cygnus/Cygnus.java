package net.onelitefeather.cygnus;

import de.icevizion.aves.util.Strings;
import de.icevizion.aves.util.TimeFormat;
import de.icevizion.aves.util.functional.VoidConsumer;
import de.icevizion.xerus.api.phase.LinearPhaseSeries;
import de.icevizion.xerus.api.phase.Phase;
import de.icevizion.xerus.api.phase.TimedPhase;
import de.icevizion.xerus.api.team.Team;
import de.icevizion.xerus.api.team.TeamService;
import de.icevizion.xerus.api.team.TeamServiceImpl;
import net.infumia.agones4j.Agones;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.listener.EntityActionListener;
import net.minestom.server.network.packet.client.play.ClientEntityActionPacket;
import net.minestom.server.utils.PacketUtils;
import net.onelitefeather.cygnus.ambient.AmbientProvider;
import net.onelitefeather.cygnus.command.StartCommand;
import net.onelitefeather.cygnus.common.ListenerHandling;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.config.GameConfigReader;
import net.onelitefeather.cygnus.common.event.GamePreLaunchEvent;
import net.onelitefeather.cygnus.common.map.MapProvider;
import net.onelitefeather.cygnus.common.page.PageProvider;
import net.onelitefeather.cygnus.common.page.event.PageEvent;
import net.onelitefeather.cygnus.common.util.Helper;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.SlenderReviveEvent;
import net.onelitefeather.cygnus.listener.PlayerChatListener;
import net.onelitefeather.cygnus.listener.PlayerDeathListener;
import net.onelitefeather.cygnus.listener.PlayerItemListener;
import net.onelitefeather.cygnus.listener.PlayerLoginListener;
import net.onelitefeather.cygnus.listener.PlayerQuitListener;
import net.onelitefeather.cygnus.listener.PlayerSpawnListener;
import net.onelitefeather.cygnus.listener.game.GameFinishListener;
import net.onelitefeather.cygnus.listener.game.GamePageListener;
import net.onelitefeather.cygnus.listener.game.GamePreLaunchListener;
import net.onelitefeather.cygnus.listener.game.GameReviveListener;
import net.onelitefeather.cygnus.listener.game.PlayerPageInteractListener;
import net.onelitefeather.cygnus.listener.game.PlayerStartSprintingListener;
import net.onelitefeather.cygnus.listener.game.PlayerStopSprintingListener;
import net.onelitefeather.cygnus.movement.CygnusEntityActionListener;
import net.onelitefeather.cygnus.movement.PlayerStartSprintingEvent;
import net.onelitefeather.cygnus.movement.PlayerStopSprintingEvent;
import net.onelitefeather.cygnus.phase.GamePhase;
import net.onelitefeather.cygnus.phase.LobbyPhase;
import net.onelitefeather.cygnus.phase.RestartPhase;
import net.onelitefeather.cygnus.phase.WaitingPhase;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.onelitefeather.cygnus.stamina.StaminaService;
import net.onelitefeather.cygnus.utils.Items;
import net.onelitefeather.cygnus.utils.StaminaHelper;
import net.onelitefeather.cygnus.utils.TeamHelper;
import net.onelitefeather.cygnus.utils.ViewRuleUpdater;
import net.onelitefeather.cygnus.view.GameView;
import net.onelitefeather.cygnus.view.GameViewImpl;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.nio.file.Paths;
import java.util.function.Supplier;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
@SuppressWarnings("java:S3252")
public final class Cygnus implements TeamCreator, ListenerHandling {
    private final Path path;
    private final TeamService<Team> teamService;
    private final LinearPhaseSeries<TimedPhase> linearPhaseSeries;
    private final AmbientProvider ambientProvider;
    private final StaminaService staminaService;
    private final Items items;
    private PageProvider pageProvider;
    private GameView view;
    private MapProvider mapProvider;
    private GameConfig gameConfig;
    private final Agones agones;

    public Cygnus(Agones agones) {
        this.agones = agones;
        this.path = Paths.get("");
        this.teamService = new TeamServiceImpl<>();
        this.linearPhaseSeries = new LinearPhaseSeries<>("game");
        this.items = new Items();
        this.ambientProvider = new AmbientProvider();
        this.staminaService = new StaminaService();
        this.gameConfig = new GameConfigReader(Paths.get("")).getConfig();
        MinecraftServer.getConnectionManager().setPlayerProvider(CygnusPlayer::new);
        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        MinecraftServer.getInstanceManager().registerInstance(instance);
        this.pageProvider = new PageProvider(this::handleAllPageFound);
        this.mapProvider = new MapProvider(path, instance, this.pageProvider);
        this.mapProvider.prepareInstanceData(instance);
        this.view = new GameViewImpl(this::getViewComponent);
        this.createTeams(this.gameConfig, this.teamService, this.ambientProvider);
        this.initPhases();
        this.initCommands();
        this.initListener();
        this.linearPhaseSeries.start();
        this.registerGameListener();
    }
    private void initCommands() {
        var manager = MinecraftServer.getCommandManager();
        manager.register(new StartCommand(this.linearPhaseSeries));
    }

    private void handleAllPageFound() {
        var gamePhase = (GamePhase) this.linearPhaseSeries.getCurrentPhase();
        gamePhase.setFinishEvent(new GameFinishEvent(GameFinishEvent.Reason.ALL_PAGES_FOUND));
        gamePhase.finish();
    }

    private void initListener() {
        Supplier<Phase> phaseSupplier = this.linearPhaseSeries::getCurrentPhase;
        var manager = MinecraftServer.getGlobalEventHandler();
        manager.addListener(PlayerSpawnEvent.class, new PlayerSpawnListener(this.mapProvider, phaseSupplier));
        PlayerQuitListener quitListener = new PlayerQuitListener(phaseSupplier, teamService, this.staminaService::forceStopSlenderBar, this.gameConfig.minPlayers());
        manager.addListener(PlayerDisconnectEvent.class, quitListener);
        manager.addListener(AsyncPlayerConfigurationEvent.class,
                new PlayerLoginListener(
                        mapProvider.getInstance(),
                        this.gameConfig.maxPlayers()
                )
        );
        manager.addListener(PlayerChatEvent.class, new PlayerChatListener());
        registerCancelListener(manager);
    }

    private void registerGameListener() {
        Supplier<Phase> phaseSupplier = this.linearPhaseSeries::getCurrentPhase;
        var manager = MinecraftServer.getGlobalEventHandler();
        manager.addListener(GameFinishEvent.class, new GameFinishListener());
        manager.addListener(PlayerUseItemEvent.class, new PlayerItemListener(this.staminaService, this::triggerViewRuleUpdate));
        manager.addListener(PlayerDeathEvent.class, new PlayerDeathListener(phaseSupplier, this.teamService));
        manager.addListener(PlayerEntityInteractEvent.class, new PlayerPageInteractListener(this.pageProvider));
        manager.addListener(PageEvent.class, new GamePageListener(this.pageProvider));
        manager.addListener(PlayerStartSprintingEvent.class, new PlayerStartSprintingListener(this.staminaService::getFoodBar));
        manager.addListener(PlayerStopSprintingEvent.class, new PlayerStopSprintingListener(this.staminaService::getFoodBar));
        manager.addListener(
                SlenderReviveEvent.class, new GameReviveListener(this.mapProvider.getGameMap(), this.items, this.staminaService));
        manager.addListener(GamePreLaunchEvent.class, new GamePreLaunchListener(this.pageProvider::setMaxPageAmount));
        MinecraftServer.getPacketListenerManager().setListener(ClientEntityActionPacket.class, CygnusEntityActionListener::listener);
    }

    private void initPhases() {
        VoidConsumer gameMapLoader = this.mapProvider::loadGameMap;
        VoidConsumer staminaInitializer = () -> StaminaHelper.initStaminaObjects(this.teamService, this.staminaService);
        VoidConsumer worldUpdater = this.mapProvider::setMidnight;
        VoidConsumer instanceSwitch = this.mapProvider::switchToGameMap;
        VoidConsumer teamInitializer = () -> TeamHelper.teleportTeams(
                this.teamService,
                this.mapProvider.getGameMap(),
                this.mapProvider.getGameInstance()
        );
        LobbyPhase lobbyPhase = new LobbyPhase(gameMapLoader, staminaInitializer, worldUpdater, this.gameConfig.lobbyTime(), this.gameConfig.minPlayers(), agones);
        this.linearPhaseSeries.add(lobbyPhase);
        this.linearPhaseSeries.add(new WaitingPhase(this.view, instanceSwitch, teamInitializer));
        this.linearPhaseSeries.add(new GamePhase(this.view, this::triggerGameStart, this::finishGame, worldUpdater, this.gameConfig.gameTime()));
        this.linearPhaseSeries.add(new RestartPhase());
    }

    private void finishGame() {
        this.pageProvider.cleanUp();
        this.staminaService.cleanUp();
        this.ambientProvider.stopTask();
        this.staminaService.cleanUp();
        MinecraftServer.getPacketListenerManager().setListener(ClientEntityActionPacket.class, EntityActionListener::listener);
    }

    private @NotNull Component getViewComponent() {
        var gamePhase = (GamePhase) this.linearPhaseSeries.getCurrentPhase();
        return Messages.getViewComponent(
                Strings.getTimeString(TimeFormat.MM_SS, gamePhase.getCurrentTicks()),
                this.pageProvider.getPageStatus()
        );
    }

    private void triggerGameStart() {
        var slenderPlayer = this.teamService.getTeams().get(Helper.SLENDER_ID).getPlayers().stream().findFirst().get();
        slenderPlayer.setTag(Tags.HIDDEN, (byte) 1);
        slenderPlayer.sendMessage(Messages.SLENDER_JOIN_PART);
        this.items.setSlenderEye(slenderPlayer);
        this.staminaService.start();
        this.pageProvider.spawn();
        this.ambientProvider.startTask();
        var message = Messages.getSurvivorJoinMessage(String.valueOf(this.pageProvider.getMaxPageAmount()));
        this.teamService.getTeams().get(Helper.SURVIVOR_ID).getPlayers().forEach(player -> {
            player.sendMessage(message);
            player.setTag(Tags.HIDDEN, (byte) 0);
        });
        TeamHelper.updateTabList(this.teamService);
        PacketUtils.broadcastPlayPacket(slenderPlayer.getMetadataPacket());
        MinecraftServer.getConnectionManager().getOnlinePlayers().stream().filter(p -> !p.getUuid().equals(slenderPlayer.getUuid())).forEach(p -> {
            slenderPlayer.updateOldViewer(p);
        });
        MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(player -> player.setRespawnPoint(mapProvider.getGameMap().getSpawn()));
        PacketUtils.broadcastPlayPacket(slenderPlayer.getMetadataPacket());
    }

    private void triggerViewRuleUpdate(@NotNull Player player) {
        ViewRuleUpdater.updateViewer(player, this.teamService.getTeams().get(Helper.SURVIVOR_ID));
    }

    public Agones getAgones() {
        return agones;
    }
}
