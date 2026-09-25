package net.onelitefeather.cygnus.listener.game;

import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.timer.TaskSchedule;
import net.onelitefeather.cygnus.ambient.AmbientProvider;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.page.PageProvider;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.page.PageProximityService;
import net.onelitefeather.cygnus.common.page.event.PageSpawnEvent;
import net.onelitefeather.cygnus.stamina.SlenderBarHelper;
import net.onelitefeather.cygnus.stamina.StaminaService;
import net.onelitefeather.cygnus.team.TeamHelper;
import net.onelitefeather.cygnus.utils.Items;
import net.onelitefeather.cygnus.visibility.VisibilityRules;
import net.theevilreaper.xerus.api.team.Team;
import net.theevilreaper.xerus.api.team.TeamService;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public final class GameStartListener implements Consumer<GameStartEvent> {

    private static final int TICKS_PER_SECOND = 20;

    private final TeamService teamService;
    private final AmbientProvider ambientProvider;
    private final StaminaService staminaService;
    private final PageProvider pageProvider;
    private final PageProximityService pageProximityService;

    public GameStartListener(TeamService teamService, AmbientProvider ambientProvider, StaminaService staminaService, PageProvider pageProvider, PageProximityService pageProximityService) {
        this.teamService = teamService;
        this.ambientProvider = ambientProvider;
        this.staminaService = staminaService;
        this.pageProvider = pageProvider;
        this.pageProximityService = pageProximityService;
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

        // Hiding the slender goes exclusively through the viewable rule. The previous
        // updateOldViewer/broadcastPlayPacket combination only sent packets: it left the viewer bit set
        // untouched, so the next rule evaluation considered every player still registered and skipped the
        // spawn packet when the slender revealed themselves again.
        VisibilityRules.refresh(slenderPlayer);
    }

    private void handleSurvivorStart() {
        Team survivorTeam = this.teamService.getTeam(GameConfig.SURVIVOR_KEY)
                .orElseThrow(() -> new IllegalStateException("Survivor team is missing"));
        Component message = Messages.getSurvivorJoinMessage(String.valueOf(this.pageProvider.getMaxPageAmount()));
        survivorTeam.getPlayers().forEach(player -> {
            player.sendMessage(message);
            player.setTag(Tags.HIDDEN, SlenderBarHelper.VISIBLE);
        });
    }

    private void startGlobalMechanics() {
        this.staminaService.start();
        this.ambientProvider.startTask();
        // Delayed so survivors get a moment to move away from the spawn point before the first
        // pages appear, instead of one being reachable the instant the round starts. The proximity
        // task starts alongside it, in the same task, since it depends on pages already existing.
        // Jittered so the moment doesn't land on the exact same tick every round.
        MinecraftServer.getSchedulerManager().buildTask(() -> {
            EventDispatcher.call(new PageSpawnEvent());
            this.pageProximityService.startTask();
        }).delay(TaskSchedule.tick(randomizedSpawnDelayTicks())).schedule();
        TeamHelper.updateTabList(this.teamService);
    }

    private int randomizedSpawnDelayTicks() {
        int baseTicks = GameConfig.PAGE_SPAWN_DELAY * TICKS_PER_SECOND;
        int jitterTicks = GameConfig.PAGE_SPAWN_DELAY_JITTER * TICKS_PER_SECOND;
        ThreadLocalRandom current = ThreadLocalRandom.current();
        int offset = current.nextInt(2 * jitterTicks + 1) - jitterTicks;
        return baseTicks + offset;
    }
}
