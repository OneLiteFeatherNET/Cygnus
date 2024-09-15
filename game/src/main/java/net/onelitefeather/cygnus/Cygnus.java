package net.onelitefeather.cygnus;

import de.icevizion.aves.util.Strings;
import de.icevizion.aves.util.TimeFormat;
import de.icevizion.xerus.api.phase.LinearPhaseSeries;
import de.icevizion.xerus.api.phase.TimedPhase;
import de.icevizion.xerus.api.team.Team;
import de.icevizion.xerus.api.team.TeamService;
import de.icevizion.xerus.api.team.TeamServiceImpl;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.extensions.Extension;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.listener.EntityActionListener;
import net.minestom.server.network.packet.client.play.ClientEntityActionPacket;
import net.minestom.server.utils.PacketUtils;
import net.onelitefeather.cygnus.ambient.AmbientProvider;
import net.onelitefeather.cygnus.command.StartCommand;
import net.onelitefeather.cygnus.common.ListenerHandling;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.map.MapProvider;
import net.onelitefeather.cygnus.common.page.PageProvider;
import net.onelitefeather.cygnus.common.page.event.PageEvent;
import net.onelitefeather.cygnus.common.util.Helper;
import net.onelitefeather.cygnus.config.ConfigHolder;
import net.onelitefeather.cygnus.config.GameConfig;
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
import net.onelitefeather.cygnus.stamina.StaminaService;
import net.onelitefeather.cygnus.utils.Items;
import net.onelitefeather.cygnus.view.GameView;
import net.onelitefeather.cygnus.view.GameViewImpl;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
@SuppressWarnings("java:S3252")
public final class Cygnus extends Extension implements TeamCreator, ListenerHandling {

    private static final GameConfig GAME_CONFIG = new GameConfig(1, 12);
    private final TeamService<Team> teamService;
    private final LinearPhaseSeries<TimedPhase> linearPhaseSeries;
    private final AmbientProvider ambientProvider;
    private final StaminaService staminaService;
    private final Items items;
    private PageProvider pageProvider;
    private GameView view;
    private MapProvider mapProvider;

    public Cygnus() {
        this.teamService = new TeamServiceImpl<>();
        this.linearPhaseSeries = new LinearPhaseSeries<>("game");
        this.items = new Items();
        this.ambientProvider = new AmbientProvider();
        this.staminaService = new StaminaService();
    }

    @Override
    public void initialize() {
        new ConfigHolder(getDataDirectory());
        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        MinecraftServer.getInstanceManager().registerInstance(instance);
        this.pageProvider = new PageProvider(this.linearPhaseSeries);
        this.mapProvider = new MapProvider(getDataDirectory(), instance, this.pageProvider, false);
        this.mapProvider.prepareInstanceData(instance);
        this.view = new GameViewImpl(this::getViewComponent);
        this.createTeams(GAME_CONFIG, this.teamService, this.ambientProvider);
        this.initPhases();
        this.initCommands();
        this.initListener();
        this.linearPhaseSeries.start();
        this.registerGameListener();
    }

    @Override
    public void terminate() {
        // Nothing to do here for the moment
    }

    private void initCommands() {
        var manager = MinecraftServer.getCommandManager();
        manager.register(new StartCommand(this.linearPhaseSeries));
    }

    private void initListener() {
        var manager = MinecraftServer.getGlobalEventHandler();
        manager.addListener(PlayerSpawnEvent.class, new PlayerSpawnListener(this.mapProvider, this.linearPhaseSeries));
        manager.addListener(PlayerDisconnectEvent.class, new PlayerQuitListener(linearPhaseSeries, teamService, this.staminaService::forceStopSlenderBar));
        manager.addListener(AsyncPlayerConfigurationEvent.class, new PlayerLoginListener(mapProvider.getInstance(), GameConfig.MAX_PLAYERS));
        manager.addListener(PlayerChatEvent.class, new PlayerChatListener(this.linearPhaseSeries.getCurrentPhase()));
        registerCancelListener(manager);
    }

    private void registerGameListener() {
        var manager = MinecraftServer.getGlobalEventHandler();
        manager.addListener(GameFinishEvent.class, new GameFinishListener());
        manager.addListener(PlayerUseItemEvent.class, new PlayerItemListener(this.staminaService, this::triggerViewRuleUpdate));
        manager.addListener(PlayerDeathEvent.class, new PlayerDeathListener(this.linearPhaseSeries, this.teamService));
        manager.addListener(PlayerEntityInteractEvent.class, new PlayerPageInteractListener(this.pageProvider));
        manager.addListener(PageEvent.class, new GamePageListener(this.pageProvider));
        manager.addListener(PlayerStartSprintingEvent.class, new PlayerStartSprintingListener(this.staminaService::getFoodBar));
        manager.addListener(PlayerStopSprintingEvent.class, new PlayerStopSprintingListener(this.staminaService::getFoodBar));
        manager.addListener(
                SlenderReviveEvent.class, new GameReviveListener(this.mapProvider.getGameMap(), this.items, this.staminaService));
        MinecraftServer.getPacketListenerManager().setListener(ClientEntityActionPacket.class, CygnusEntityActionListener::listener);
    }

    private void initPhases() {
        Consumer<Void> worldUpdater = unused -> this.mapProvider.setMidnight();
        this.linearPhaseSeries.add(new LobbyPhase(
                this.teamService.getTeams()::get,
                this.staminaService,
                worldUpdater,
                this.mapProvider
        ));
        this.linearPhaseSeries.add(new WaitingPhase(this.view, this.mapProvider, this.teamService.getTeams()::get));
        this.linearPhaseSeries.add(new GamePhase(this.view, this::triggerGameStart, this::finishGame, worldUpdater));
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

    private void setMainInstance(@NotNull Player player, @NotNull Instance instance) {
        player.setInstance(instance, Vec.ZERO);
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
        Helper.updateTabList(this.teamService);
        PacketUtils.broadcastPlayPacket(slenderPlayer.getMetadataPacket());
        MinecraftServer.getConnectionManager().getOnlinePlayers().stream().filter(p -> !p.getUuid().equals(slenderPlayer.getUuid())).forEach(p -> {
            slenderPlayer.updateOldViewer(p);
        });
        MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(player -> player.setRespawnPoint(mapProvider.getGameMap().getSpawn()));
        PacketUtils.broadcastPlayPacket(slenderPlayer.getMetadataPacket());
    }

    private void triggerViewRuleUpdate(@NotNull Player player) {
        //  ViewRuleUpdater.updateViewer(player, this.teamService.getTeams().get(Helper.SURVIVOR_ID));
    }
}
