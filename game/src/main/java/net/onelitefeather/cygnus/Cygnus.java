package net.onelitefeather.cygnus;

import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.instance.Instance;
import net.onelitefeather.cygnus.common.page.event.PageDiscoveryCompletedEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.common.page.event.PageSpawnEvent;
import net.onelitefeather.cygnus.listener.game.GameStartListener;
import net.onelitefeather.cygnus.listener.map.GameMapLoadedListener;
import net.onelitefeather.cygnus.listener.page.PageSpawnListener;
import net.onelitefeather.cygnus.listener.view.ViewUpdateListener;
import net.onelitefeather.cygnus.listener.page.PageDiscoveryCompleteListener;
import net.onelitefeather.cygnus.map.GameMapProvider;
import net.onelitefeather.cygnus.map.event.GameMapLoadEvent;
import net.onelitefeather.cygnus.map.event.GameMapLoadedEvent;
import net.onelitefeather.cygnus.map.event.GamePrepareEvent;
import net.onelitefeather.cygnus.overlay.OverlayModule;
import net.onelitefeather.cygnus.spectator.SpectatorService;
import net.onelitefeather.cygnus.creek.CreekModule;
import net.onelitefeather.cygnus.creek.dread.DreadSource;
import net.onelitefeather.cygnus.team.TeamCreator;
import net.onelitefeather.cygnus.team.TeamHelper;
import net.onelitefeather.cygnus.view.event.ViewUpdateEvent;
import net.theevilreaper.aves.util.functional.VoidConsumer;
import net.theevilreaper.xerus.api.phase.LinearPhaseSeries;
import net.theevilreaper.xerus.api.phase.Phase;
import net.theevilreaper.xerus.api.phase.TimedPhase;
import net.theevilreaper.xerus.api.team.Team;
import net.theevilreaper.xerus.api.team.TeamService;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.entity.EntityType;
import net.minestom.server.listener.EntityActionListener;
import net.minestom.server.listener.common.SettingsListener;
import net.minestom.server.network.packet.client.common.ClientSettingsPacket;
import net.minestom.server.network.packet.client.play.ClientEntityActionPacket;
import net.onelitefeather.cygnus.adrenaline.AdrenalineService;
import net.onelitefeather.cygnus.ambient.AmbientProvider;
import net.onelitefeather.cygnus.page.PageProximityService;
import net.onelitefeather.cygnus.damage.DamageSoundService;
import net.onelitefeather.cygnus.command.StartCommand;
import net.onelitefeather.cygnus.common.ListenerHandling;
import net.onelitefeather.cygnus.common.bootstrap.ServiceBootstrap;
import net.onelitefeather.cygnus.common.bootstrap.ServiceShutdown;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.config.GameConfigReader;
import net.onelitefeather.cygnus.common.event.GamePreLaunchEvent;
import net.onelitefeather.cygnus.common.page.PageProvider;
import net.onelitefeather.cygnus.common.page.event.PageExpiredEvent;
import net.onelitefeather.cygnus.disclaimer.EpilepsyDisclaimer;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.SlenderReviveEvent;
import net.onelitefeather.cygnus.event.StaminaStateChangeEvent;
import net.onelitefeather.cygnus.jumpscare.JumpScareManager;
import net.onelitefeather.cygnus.listener.CygnusSettingsListener;
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
import net.onelitefeather.cygnus.listener.game.SlenderTakeover;
import net.onelitefeather.cygnus.monitoring.SentrySupport;
import net.onelitefeather.cygnus.movement.CygnusEntityActionListener;
import net.onelitefeather.cygnus.movement.PlayerStartSprintingEvent;
import net.onelitefeather.cygnus.movement.PlayerStopSprintingEvent;
import net.onelitefeather.cygnus.phase.GamePhase;
import net.onelitefeather.cygnus.phase.LobbyPhase;
import net.onelitefeather.cygnus.phase.RestartPhase;
import net.onelitefeather.cygnus.phase.WaitingPhase;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.onelitefeather.cygnus.resourcepack.ResourcePackService;
import net.onelitefeather.cygnus.sanity.SanityService;
import net.onelitefeather.cygnus.stamina.SlenderBarTrigger;
import net.onelitefeather.cygnus.stamina.StaminaService;
import net.onelitefeather.cygnus.utils.ScoreboardDisplay;
import net.onelitefeather.cygnus.utils.StaminaHelper;
import net.onelitefeather.cygnus.view.GameView;
import net.onelitefeather.cygnus.view.GameViewImpl;

import net.onelitefeather.cygnus.telemetry.ActionTracer;
import net.onelitefeather.cygnus.telemetry.CygnusAttributes;
import net.onelitefeather.cygnus.telemetry.TraceCookie;
import net.onelitefeather.cygnus.telemetry.TracingCreekWitness;
import net.onelitefeather.cygnus.creek.tab.HuntedTabWitness;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;
import net.onelitefeather.cygnus.telemetry.CygnusTracing;
import net.onelitefeather.cygnus.telemetry.JoinTracer;
import net.onelitefeather.cygnus.telemetry.KickTracer;
import net.onelitefeather.cygnus.telemetry.RoundTracer;
import net.onelitefeather.cygnus.telemetry.ShutdownTracer;
import net.onelitefeather.cygnus.telemetry.SlowTickTracer;
import net.onelitefeather.cygnus.telemetry.TickSections;
import net.onelitefeather.cygnus.telemetry.TraceStep;
import net.onelitefeather.cygnus.telemetry.TracedPhaseSeries;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * @author theEvilReaper
 * @version 1.3.0
 * @since 1.0.0
 **/
@SuppressWarnings("java:S3252")
public final class Cygnus implements TeamCreator, ListenerHandling {

    private final TeamService teamService;
    private final LinearPhaseSeries<TimedPhase> linearPhaseSeries;
    private final AmbientProvider ambientProvider;
    private final StaminaService staminaService;
    private final PageProvider pageProvider;
    private final PageProximityService pageProximityService;
    private final GameView view;
    private final GameMapProvider mapProvider;
    private final GameConfig gameConfig;
    private final JumpScareManager jumpscareManager;
    private final SpectatorService spectatorService;
    private final Optional<ResourcePackService> resourcePackService;
    private final ScoreboardDisplay scoreboardDisplay;
    private final SlenderTakeover slenderTakeover;
    private final List<GameFeature> features;

    private final CygnusTracing tracing;
    private final RoundTracer roundTracer;
    private final JoinTracer joinTracer;
    private final SlowTickTracer slowTickTracer;
    private final ActionTracer actionTracer;
    private final TraceCookie traceCookie;

    /**
     * Wires the game together.
     * <p>
     * This is the composition root, so it is also where the tracer is taken from
     * {@code GlobalOpenTelemetry} - once, and handed to everything that creates spans. The whole
     * construction is one {@code cygnus.startup} span with a child per major step. It ends when the
     * game is wired up; binding the port happens afterwards, in {@link CygnusLoader}, and is not part of it.
     * </p>
     */
    public Cygnus() {
        this.tracing = CygnusTracing.fromGlobal(serviceVersion());
        this.roundTracer = new RoundTracer(this.tracing);
        this.joinTracer = new JoinTracer(this.tracing, this.roundTracer, System::nanoTime, JoinTracer.DEFAULT_TIMEOUT);
        this.traceCookie = new TraceCookie(this.roundTracer, this.joinTracer);
        this.actionTracer = new ActionTracer(this.roundTracer, Clock.systemUTC(), this::activeMapName);
        KickTracer kickTracer = new KickTracer(this.tracing, this.roundTracer);
        TickSections tickSections = TickSections.measuring(System::nanoTime);
        // A shutdown in the middle of a round or a tick would otherwise leave its spans open; this
        // ends them before ServiceShutdown tells the JVM to exit, which is what flushes the exporter.
        ServiceShutdown.observe(new ShutdownTracer(this.tracing, this.roundTracer));
        TraceStep startup = this.tracing.root(CygnusAttributes.SPAN_STARTUP);
        this.roundTracer.startupContext(startup.span().getSpanContext());
        try {
            Path path = ServiceBootstrap.resolveWorkingDirectory();
            this.teamService = TeamService.of();
            this.linearPhaseSeries = new TracedPhaseSeries<>("game", this.roundTracer);
            this.jumpscareManager = new JumpScareManager();
            try (TraceStep ignored = startup.child("cygnus.startup.config")) {
                this.gameConfig = new GameConfigReader(path).getConfig();
            }
            this.slowTickTracer = new SlowTickTracer(this.tracing, this.roundTracer, this.gameConfig.telemetry(), Clock.systemUTC(), tickSections);
            this.staminaService = new StaminaService(this.gameConfig.stamina());
            // Set up as early as possible so anything that goes wrong while the rest of the game is
            // being wired up is already covered. Stays off entirely when no DSN is configured.
            SentrySupport.init(this.gameConfig.sentryDsn());
            try (TraceStep ignored = startup.child("cygnus.startup.resourcepack")) {
                this.resourcePackService = ResourcePackService.create(this.gameConfig.resourcePack());
            }
            // Every player needs the pack id so it can hand the pack back when it is kicked; see
            // CygnusPlayer#kick. Null when the ResourcePack feature is off, which leaves the kick untouched.
            UUID resourcePackId = this.resourcePackService.map(ResourcePackService::packId).orElse(null);
            MinecraftServer.getConnectionManager().setPlayerProvider(
                    (connection, gameProfile) -> new CygnusPlayer(connection, gameProfile, resourcePackId, kickTracer));
            this.pageProvider = new PageProvider();
            try (TraceStep ignored = startup.child("cygnus.startup.maps")) {
                this.mapProvider = new GameMapProvider(path, this.gameConfig.lobbyAtmosphereShare());
            }
            // Falco keeps its region files open, so the loaders have to be released on shutdown
            MinecraftServer.getSchedulerManager().buildShutdownTask(this.mapProvider::close);
            this.view = new GameViewImpl();
            this.createTeams(this.gameConfig.teams(), this.teamService);
            this.scoreboardDisplay = new ScoreboardDisplay(this.teamService.getTeams());
            Team survivorTeam = this.teamService.getTeam(GameConfig.SURVIVOR_KEY)
                    .orElseThrow(() -> new IllegalStateException("Survivor team not found"));
            this.ambientProvider = new AmbientProvider(survivorTeam, this.actionTracer.blackoutObserver("survivor"));
            // Only survivors get the hint: the slender hearing it would turn every page into a place to
            // camp at, which is the opposite of what the hint is for.
            this.pageProximityService = new PageProximityService(
                    this.gameConfig.pageProximity(),
                    survivorTeam::getPlayers,
                    this.pageProvider::interactablePages
            );
            Team spectatorTeam = this.teamService.getTeam(GameConfig.SPECTATOR_KEY)
                    .orElseThrow(() -> new IllegalStateException("Spectator team not found"));
            this.spectatorService = new SpectatorService(spectatorTeam, survivorTeam);
            // The creek reads the fear and reports back to it; pages and deaths reach it as events.
            SanityService sanityService = new SanityService(
                    this.gameConfig.sanity(),
                    this.pageProvider::foundShare,
                    this.gameConfig.round().gameTime() * 1000L,
                    System::currentTimeMillis,
                    () -> TeamHelper.survivorsOf(this.teamService),
                    this.actionTracer.sanityObserver(Cygnus::survivorActor));
            this.slenderTakeover = new SlenderTakeover(this.teamService, this.linearPhaseSeries::getCurrentPhase);
            try (TraceStep ignored = startup.child("cygnus.startup.features")) {
                this.features = Stream.concat(this.resourcePackService.stream(), Stream.of(
                        new EpilepsyDisclaimer(),
                        this.slenderTakeover,
                        this.spectatorService,
                        // Not part of the OverlayModule: the sound is the feedback a hit owes the player
                        // either way, and it needs neither the resource pack nor the overlay gate to be heard.
                        new DamageSoundService(this.gameConfig.damageSound(), System::currentTimeMillis),
                        new CreekModule(this.gameConfig.creek(), this.teamService, this.mapProvider,
                                sanityService, new HuntedTabWitness(new TracingCreekWitness(sanityService, this.actionTracer, Cygnus::survivorActor),
                                        MinecraftServer.getConnectionManager()::getOnlinePlayerByUuid), this.jumpscareManager, this.staminaService, tickSections),
                        sanityService,
                        new AdrenalineService(
                                this.gameConfig.adrenaline(),
                                () -> TeamHelper.survivorsOf(this.teamService),
                                System::currentTimeMillis),
                        new OverlayModule(this.gameConfig.glitch(), this.gameConfig.pageGlitch(), this.teamService,
                                this.staminaService, tickSections)
                )).toList();
            }
            try (TraceStep ignored = startup.child("cygnus.startup.phases")) {
                this.initPhases(sanityService);
            }
            try (TraceStep ignored = startup.child("cygnus.startup.listeners")) {
                this.initCommands();
                this.initListener(spectatorTeam);
                this.registerGameListener();
            }
            // Last, so the lobby only opens - and the round span only starts - once everything above
            // is in place.
            this.linearPhaseSeries.start();
        } catch (RuntimeException | Error throwable) {
            startup.fail(throwable);
            throw throwable;
        } finally {
            startup.close();
        }
    }

    private @Nullable String activeMapName() {
        GameMap activeMap = this.mapProvider.getGameMap();
        return activeMap == null ? null : activeMap.name();
    }

    private static ActionTracer.@Nullable Actor survivorActor(UUID uuid) {
        Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(uuid);
        return player == null ? null : ActionTracer.Actor.of(player);
    }

    /**
     * Returns the version this jar was built as, for the instrumentation scope.
     *
     * @return the version from the jar manifest, or {@code unknown} when not run from a jar
     */
    private static String serviceVersion() {
        String version = Cygnus.class.getPackage().getImplementationVersion();
        return version == null ? "unknown" : version;
    }

    private void initCommands() {
        var manager = MinecraftServer.getCommandManager();
        manager.register(new StartCommand(this.linearPhaseSeries));
        this.features.forEach(feature -> feature.registerCommands(manager));
    }


    private void initListener(Team spectatorTeam) {
        Supplier<Phase> phaseSupplier = this.linearPhaseSeries::getCurrentPhase;
        var manager = MinecraftServer.getGlobalEventHandler();
        // First, so the join span exists before any listener below can turn the player away.
        this.joinTracer.register(manager);
        this.traceCookie.register(manager, TraceCookie.DEFAULT_TIMEOUT);
        manager.addListener(GameMapLoadedEvent.class, event ->
                this.pageProvider.loadPageData(event.gameMap().getPageFaces())
        );
        manager.addListener(GameMapLoadedEvent.class, new GameMapLoadedListener());
        manager.addListener(PlayerSpawnEvent.class, new PlayerSpawnListener(player -> this.mapProvider.teleportToSpawn(player, false), phaseSupplier));
        PlayerQuitListener quitListener = new PlayerQuitListener(phaseSupplier, teamService, this.staminaService, this.spectatorService::updateInventory, this.slenderTakeover);
        manager.addListener(PlayerDisconnectEvent.class, quitListener);
        manager.addListener(AsyncPlayerConfigurationEvent.class,
                new PlayerLoginListener(
                        this.mapProvider.getActiveInstance(),
                        this.gameConfig.round().maxPlayers(),
                        linearPhaseSeries::getCurrentPhase,
                        this.resourcePackService
                )
        );
        manager.addListener(PlayerChatEvent.class, new PlayerChatListener(spectatorTeam));
        manager.addListener(GameMapLoadEvent.class, _ -> this.mapProvider.loadGameMap());
        manager.addListener(GamePrepareEvent.class, _ -> {
            // The roles are handed out inside initStaminaObjects; the name tag teams can only mirror
            // them afterwards.
            StaminaHelper.initStaminaObjects(this.teamService, this.staminaService);
            this.scoreboardDisplay.sync(this.teamService);
        });
        registerCancelListener(manager);
    }

    private void registerGameListener() {
        Supplier<Phase> phaseSupplier = this.linearPhaseSeries::getCurrentPhase;
        GlobalEventHandler handler = MinecraftServer.getGlobalEventHandler();
        // Before the listeners below: the death listener strips the team tag the round tracer reads
        // the role from.
        this.roundTracer.register(handler);
        this.actionTracer.register(handler, () -> TeamHelper.slenderOf(this.teamService));
        this.slowTickTracer.register(handler);

        SlenderBarTrigger trigger = new SlenderBarTrigger(this.staminaService::getSlenderBar);
        handler.addListener(PlayerUseItemEvent.class, new SlenderItemListener(trigger));
        handler.addListener(GameFinishEvent.class, new GameFinishListener());
        handler.addListener(GameStartEvent.class, new GameStartListener(this.teamService, this.ambientProvider, this.staminaService, this.pageProvider, this.pageProximityService));
        handler.addListener(PageSpawnEvent.class, new PageSpawnListener(this.pageProvider, this.mapProvider.getActiveInstance()));
        handler.addListener(PlayerDeathEvent.class, new PlayerDeathListener(
                phaseSupplier, this.teamService, this.jumpscareManager, this.staminaService, this.spectatorService::updateInventory
        ));
        handler.addListener(PlayerEntityInteractEvent.class, new PlayerPageInteractListener(this.pageProvider));
        handler.addListener(PageExpiredEvent.class, new GamePageListener(this.pageProvider));
        handler.addListener(PlayerStartSprintingEvent.class, new PlayerStartSprintingListener(this.staminaService::getFoodBar));
        handler.addListener(PlayerStopSprintingEvent.class, new PlayerStopSprintingListener(this.staminaService::getFoodBar));
        handler.addListener(SlenderReviveEvent.class, new SlenderReviveListener(this.mapProvider::getGameMap, this.staminaService, this.teamService));
        handler.addListener(SlenderReviveEvent.class, _ -> this.scoreboardDisplay.sync(this.teamService));
        handler.addListener(GamePreLaunchEvent.class, new GamePreLaunchListener(this.pageProvider));
        handler.addListener(StaminaStateChangeEvent.class, new StaminaStateChangeListener());
        handler.addListener(PageDiscoveryCompletedEvent.class, new PageDiscoveryCompleteListener(this.linearPhaseSeries));
        handler.addListener(ViewUpdateEvent.class, new ViewUpdateListener(this.view, this.pageProvider));
        MinecraftServer.getPacketListenerManager().setPlayListener(ClientEntityActionPacket.class, CygnusEntityActionListener::listener);
        MinecraftServer.getPacketListenerManager().setPlayListener(ClientSettingsPacket.class, CygnusSettingsListener::listener);
        GameFeatures.register(handler, this.features);
    }

    private void initPhases(DreadSource dreadSource) {
        VoidConsumer instanceSwitch = this.mapProvider::switchToGameMap;
        VoidConsumer teamInitializer = () -> {
            Instance activeInstance = this.mapProvider.getActiveInstance().get();
            if (activeInstance == null) {
                throw new IllegalStateException("Active instance not available for team teleport");
            }
            TeamHelper.teleportTeams(
                    this.teamService,
                    this.mapProvider.getGameMap(),
                    activeInstance
            );
            MinecraftServer.getSchedulerManager().scheduleNextTick(this.mapProvider::releasePreviousInstance);
        };
        LobbyPhase lobbyPhase = new LobbyPhase(this.gameConfig.round(), this.mapProvider.getActiveInstance());
        this.linearPhaseSeries.add(lobbyPhase);
        this.linearPhaseSeries.add(new WaitingPhase(this.view, instanceSwitch, teamInitializer));
        this.linearPhaseSeries.add(new GamePhase(this.view, this::finishGame, this.gameConfig.round().gameTime(), this.jumpscareManager, dreadSource));
        this.linearPhaseSeries.add(new RestartPhase());
    }

    private void finishGame() {
        this.pageProvider.cleanUp();
        this.staminaService.cleanUp();
        this.ambientProvider.stopTask();
        this.pageProximityService.stopTask();
        this.jumpscareManager.cleanUp();
        this.teamService.getTeam(GameConfig.SLENDER_KEY)
                .ifPresent(slenderTeam -> slenderTeam.getPlayers().forEach(player -> {
                    player.switchEntityType(EntityType.PLAYER);
                    player.getInventory().clear();
                }));
        // Drops the pages the survivors collected during the round
        this.teamService.getTeam(GameConfig.SURVIVOR_KEY)
                .ifPresent(survivorTeam -> survivorTeam.getPlayers()
                        .forEach(player -> player.getInventory().clear()));
        MinecraftServer.getPacketListenerManager().setPlayListener(ClientEntityActionPacket.class, EntityActionListener::listener);
        MinecraftServer.getPacketListenerManager().setPlayListener(ClientSettingsPacket.class, SettingsListener::listener);
    }
}
