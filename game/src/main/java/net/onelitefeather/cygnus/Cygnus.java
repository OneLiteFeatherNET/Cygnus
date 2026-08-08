package net.onelitefeather.cygnus;

import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.instance.Instance;
import net.onelitefeather.cygnus.common.page.event.PageDiscoveryCompletedEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.common.page.event.PageSpawnEvent;
import net.onelitefeather.cygnus.listener.game.GameStartListener;
import net.onelitefeather.cygnus.listener.page.PageSpawnListener;
import net.onelitefeather.cygnus.listener.view.ViewUpdateListener;
import net.onelitefeather.cygnus.listener.page.PageDiscoveryCompleteListener;
import net.onelitefeather.cygnus.map.GameMapProvider;
import net.onelitefeather.cygnus.map.event.GameMapLoadEvent;
import net.onelitefeather.cygnus.map.event.GameMapLoadedEvent;
import net.onelitefeather.cygnus.map.event.GamePrepareEvent;
import net.onelitefeather.cygnus.spectator.SpectatorService;
import net.onelitefeather.cygnus.team.TeamCreator;
import net.onelitefeather.cygnus.team.TeamHelper;
import net.onelitefeather.cygnus.view.event.ViewUpdateEvent;
import net.theevilreaper.aves.map.provider.AbstractMapProvider;
import net.theevilreaper.aves.util.functional.VoidConsumer;
import net.theevilreaper.xerus.api.phase.LinearPhaseSeries;
import net.theevilreaper.xerus.api.phase.Phase;
import net.theevilreaper.xerus.api.phase.TimedPhase;
import net.theevilreaper.xerus.api.team.Team;
import net.theevilreaper.xerus.api.team.TeamService;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.listener.EntityActionListener;
import net.minestom.server.network.packet.client.play.ClientEntityActionPacket;
import net.onelitefeather.cygnus.ambient.AmbientProvider;
import net.onelitefeather.cygnus.command.StartCommand;
import net.onelitefeather.cygnus.common.ListenerHandling;
import net.onelitefeather.cygnus.common.bootstrap.ServiceBootstrap;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.config.GameConfigReader;
import net.onelitefeather.cygnus.common.event.GamePreLaunchEvent;
import net.onelitefeather.cygnus.common.page.PageProvider;
import net.onelitefeather.cygnus.common.page.event.PageExpiredEvent;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.SlenderReviveEvent;
import net.onelitefeather.cygnus.event.StaminaStateChangeEvent;
import net.onelitefeather.cygnus.jumpscare.JumpScareManager;
import net.onelitefeather.cygnus.listener.PlayerChatListener;
import net.onelitefeather.cygnus.listener.PlayerDeathListener;
import net.onelitefeather.cygnus.listener.PlayerLoginListener;
import net.onelitefeather.cygnus.listener.PlayerQuitListener;
import net.onelitefeather.cygnus.listener.PlayerSpawnListener;
import net.onelitefeather.cygnus.listener.stamina.StaminaStateChangeListener;
import net.onelitefeather.cygnus.listener.game.GameFinishListener;
import net.onelitefeather.cygnus.listener.page.GamePageListener;
import net.onelitefeather.cygnus.listener.game.GamePreLaunchListener;
import net.onelitefeather.cygnus.listener.game.SlenderReviveListener;
import net.onelitefeather.cygnus.listener.page.PlayerPageInteractListener;
import net.onelitefeather.cygnus.listener.game.PlayerStartSprintingListener;
import net.onelitefeather.cygnus.listener.game.PlayerStopSprintingListener;
import net.onelitefeather.cygnus.listener.game.SlenderItemListener;
import net.onelitefeather.cygnus.movement.CygnusEntityActionListener;
import net.onelitefeather.cygnus.movement.PlayerStartSprintingEvent;
import net.onelitefeather.cygnus.movement.PlayerStopSprintingEvent;
import net.onelitefeather.cygnus.phase.GamePhase;
import net.onelitefeather.cygnus.phase.LobbyPhase;
import net.onelitefeather.cygnus.phase.RestartPhase;
import net.onelitefeather.cygnus.phase.WaitingPhase;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.onelitefeather.cygnus.resourcepack.ResourcePackService;
import net.onelitefeather.cygnus.stamina.SlenderBarTrigger;
import net.onelitefeather.cygnus.stamina.StaminaService;
import net.onelitefeather.cygnus.utils.StaminaHelper;
import net.onelitefeather.cygnus.utils.ViewRuleUpdater;
import net.onelitefeather.cygnus.view.GameView;
import net.onelitefeather.cygnus.view.GameViewImpl;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
@SuppressWarnings("java:S3252")
public final class Cygnus implements TeamCreator, ListenerHandling {

    private final TeamService teamService;
    private final LinearPhaseSeries<TimedPhase> linearPhaseSeries;
    private final AmbientProvider ambientProvider;
    private final StaminaService staminaService;
    private final PageProvider pageProvider;
    private final GameView view;
    private final AbstractMapProvider mapProvider;
    private final GameConfig gameConfig;
    private final JumpScareManager jumpscareManager;
    private final SpectatorService spectatorService;
    private final Optional<ResourcePackService> resourcePackService;

    public Cygnus() {
        Path path = ServiceBootstrap.resolveWorkingDirectory();
        this.teamService = TeamService.of();
        this.linearPhaseSeries = new LinearPhaseSeries<>("game");
        this.staminaService = new StaminaService();
        this.jumpscareManager = new JumpScareManager();
        this.gameConfig = new GameConfigReader(path).getConfig();
        MinecraftServer.getConnectionManager().setPlayerProvider(CygnusPlayer::new);
        this.pageProvider = new PageProvider();
        GameMapProvider gameMapProvider = new GameMapProvider(path);
        this.mapProvider = gameMapProvider;
        // Falco keeps its region files open, so the loaders have to be released on shutdown
        MinecraftServer.getSchedulerManager().buildShutdownTask(gameMapProvider::close);
        this.view = new GameViewImpl();
        this.createTeams(this.gameConfig, this.teamService);
        Team survivorTeam = this.teamService.getTeam(GameConfig.SURVIVOR_KEY)
                .orElseThrow(() -> new IllegalStateException("Survivor team not found"));
        this.ambientProvider = new AmbientProvider(survivorTeam);
        Team spectatorTeam = this.teamService.getTeam(GameConfig.SPECTATOR_KEY)
                .orElseThrow(() -> new IllegalStateException("Spectator team not found"));
        this.spectatorService = new SpectatorService(spectatorTeam, survivorTeam);
        this.resourcePackService = ResourcePackService.create();
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


    private void initListener() {
        Supplier<Phase> phaseSupplier = this.linearPhaseSeries::getCurrentPhase;
        var manager = MinecraftServer.getGlobalEventHandler();
        manager.addListener(GameMapLoadedEvent.class, event ->
                this.pageProvider.loadPageData(event.gameMap().getPageFaces())
        );
        manager.addListener(PlayerSpawnEvent.class, new PlayerSpawnListener(player -> this.mapProvider.teleportToSpawn(player, false), phaseSupplier));
        PlayerQuitListener quitListener = new PlayerQuitListener(phaseSupplier, teamService, this.staminaService, this.gameConfig.minPlayers());
        manager.addListener(PlayerDisconnectEvent.class, quitListener);
        manager.addListener(AsyncPlayerConfigurationEvent.class,
                new PlayerLoginListener(
                        this.mapProvider.getActiveInstance(),
                        this.gameConfig.maxPlayers(),
                        linearPhaseSeries::getCurrentPhase,
                        this.resourcePackService
                )
        );
        this.resourcePackService.ifPresent(service -> service.registerListener(manager));
        Team spectatorTeam = this.teamService.getTeam(GameConfig.SPECTATOR_KEY)
                .orElseThrow(() -> new IllegalStateException("Spectator team not found"));
        manager.addListener(PlayerChatEvent.class, new PlayerChatListener(spectatorTeam, phaseSupplier));
        manager.addListener(GameMapLoadEvent.class, _ -> ((GameMapProvider) this.mapProvider).loadGameMap());
        manager.addListener(GamePrepareEvent.class, _ -> {
            StaminaHelper.initStaminaObjects(this.teamService, this.staminaService);
        });
        registerCancelListener(manager);
    }

    private void registerGameListener() {
        Supplier<Phase> phaseSupplier = this.linearPhaseSeries::getCurrentPhase;
        GlobalEventHandler handler = MinecraftServer.getGlobalEventHandler();

        SlenderBarTrigger trigger = new SlenderBarTrigger(this.staminaService::getSlenderBar, this::triggerViewRuleUpdate);
        handler.addListener(PlayerUseItemEvent.class, new SlenderItemListener(trigger));
        handler.addListener(GameFinishEvent.class, new GameFinishListener());
        handler.addListener(GameStartEvent.class, new GameStartListener(this.teamService, this.ambientProvider, this.staminaService, this.pageProvider));
        handler.addListener(PageSpawnEvent.class, new PageSpawnListener(this.pageProvider, this.mapProvider.getActiveInstance()));
        handler.addListener(PlayerDeathEvent.class, new PlayerDeathListener(phaseSupplier, this.teamService, this.jumpscareManager));
        handler.addListener(PlayerEntityInteractEvent.class, new PlayerPageInteractListener(this.pageProvider));
        handler.addListener(PageExpiredEvent.class, new GamePageListener(this.pageProvider));
        handler.addListener(PlayerStartSprintingEvent.class, new PlayerStartSprintingListener(this.staminaService::getFoodBar));
        handler.addListener(PlayerStopSprintingEvent.class, new PlayerStopSprintingListener(this.staminaService::getFoodBar));
        handler.addListener(
                SlenderReviveEvent.class, new SlenderReviveListener(((GameMapProvider) this.mapProvider)::getGameMap, this.staminaService));
        handler.addListener(GamePreLaunchEvent.class, new GamePreLaunchListener(this.pageProvider::setMaxPageAmount));
        handler.addListener(StaminaStateChangeEvent.class, new StaminaStateChangeListener());
        handler.addListener(PageDiscoveryCompletedEvent.class, new PageDiscoveryCompleteListener(this.linearPhaseSeries));
        handler.addListener(ViewUpdateEvent.class, new ViewUpdateListener(this.view, this.pageProvider));
        MinecraftServer.getPacketListenerManager().setPlayListener(ClientEntityActionPacket.class, CygnusEntityActionListener::listener);

        spectatorService.registerListener(handler);
    }

    private void initPhases() {
        GameMapProvider gameMapProvider = ((GameMapProvider) this.mapProvider);
        VoidConsumer instanceSwitch = gameMapProvider::switchToGameMap;
        VoidConsumer teamInitializer = () -> {
            Instance activeInstance = gameMapProvider.getActiveInstance().get();
            if (activeInstance == null) {
                throw new IllegalStateException("Active instance not available for team teleport");
            }
            TeamHelper.teleportTeams(
                    this.teamService,
                    gameMapProvider.getGameMap(),
                    activeInstance
            );
            MinecraftServer.getSchedulerManager().scheduleNextTick(gameMapProvider::releasePreviousInstance);
        };
        LobbyPhase lobbyPhase = new LobbyPhase(this.gameConfig, gameMapProvider.getActiveInstance());
        this.linearPhaseSeries.add(lobbyPhase);
        this.linearPhaseSeries.add(new WaitingPhase(this.view, instanceSwitch, teamInitializer));
        this.linearPhaseSeries.add(new GamePhase(this.view, this::finishGame, this.gameConfig.gameTime(), this.jumpscareManager));
        this.linearPhaseSeries.add(new RestartPhase());
    }

    private void finishGame() {
        this.pageProvider.cleanUp();
        this.staminaService.cleanUp();
        this.ambientProvider.stopTask();
        this.jumpscareManager.cleanUp();
        MinecraftServer.getPacketListenerManager().setPlayListener(ClientEntityActionPacket.class, EntityActionListener::listener);
    }

    private void triggerViewRuleUpdate(@NotNull Player player) {
        ViewRuleUpdater.updateViewer(player, this.teamService.getTeam(GameConfig.SURVIVOR_KEY).orElseThrow());
    }
}
