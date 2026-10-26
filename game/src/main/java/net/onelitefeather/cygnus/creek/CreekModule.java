package net.onelitefeather.cygnus.creek;

import net.onelitefeather.cygnus.telemetry.TickSections;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.onelitefeather.cygnus.GameFeature;
import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.common.creek.CreekRoute;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.map.settings.CreekSettings;
import net.onelitefeather.cygnus.creek.body.CreakingBody;
import net.onelitefeather.cygnus.creek.consequence.catching.CatchEffects;
import net.onelitefeather.cygnus.creek.consequence.catching.CatchLaunch;
import net.onelitefeather.cygnus.creek.consequence.catching.CatchSpawnTeleport;
import net.onelitefeather.cygnus.creek.consequence.catching.CatchSwap;
import net.onelitefeather.cygnus.creek.consequence.catching.CatchTricks;
import net.onelitefeather.cygnus.creek.consequence.LocatorReveal;
import net.onelitefeather.cygnus.creek.consequence.RevealMarks;
import net.onelitefeather.cygnus.creek.consequence.catching.LandingDamage;
import net.onelitefeather.cygnus.creek.consequence.catching.StagedCatchConsequence;
import net.onelitefeather.cygnus.creek.dread.CreekWitness;
import net.onelitefeather.cygnus.creek.dread.DreadSource;
import net.onelitefeather.cygnus.jumpscare.JumpScareManager;
import net.onelitefeather.cygnus.map.GameMapProvider;
import net.onelitefeather.cygnus.possession.PossessionTarget;
import net.onelitefeather.cygnus.stamina.StaminaService;
import net.onelitefeather.cygnus.team.TeamHelper;
import net.theevilreaper.xerus.api.team.TeamService;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

/**
 * Wires the creek: its service and the listeners that start and stop it with the round.
 */
public final class CreekModule implements GameFeature {

    private final EventNode<Event> node = EventNode.all("creek");

    private final CreekConfig config;
    private final GameMapProvider mapProvider;
    private final CreekService service;
    private final CreekPossession possession;

    /**
     * Builds the creek, with its step measured for the slow tick report.
     *
     * @param config           the creek settings
     * @param settings         the distances the map sets for the creek
     * @param teamService      the teams of the round
     * @param mapProvider      the maps and the active instance
     * @param dread            rates how scared each survivor is
     * @param witness          hears about catches and sightings
     * @param respite          tells who the creek leaves alone for now
     * @param jumpScareManager plays the jump scare of a catch
     * @param staminaService   the stamina bars a catch drains
     * @param sections         measures how long each step takes
     * @param gameTimeSeconds  how long a round lasts, which the creek's mood switch is measured against
     * @since 2.15.0
     */
    public CreekModule(CreekConfig config, CreekSettings settings, TeamService teamService,
                       GameMapProvider mapProvider, DreadSource dread, CreekWitness witness, CreekRespite respite,
                       JumpScareManager jumpScareManager, StaminaService staminaService, TickSections sections,
                       int gameTimeSeconds) {
        this.config = config;
        this.mapProvider = mapProvider;
        // Every step and every catch runs on the scheduler thread, but Random is thread-safe
        // anyway, which keeps a stray call from elsewhere harmless.
        Random random = new Random();
        // One reveal for the catches and the marks, so the slender only ever sees the newest one.
        LocatorReveal reveal = new LocatorReveal(this.config.catching().betrayalGlowSeconds(), sections);
        MoodClock mood = new MoodClock(gameTimeSeconds * 1000L, random);
        CatchTricks tricks = new CatchTricks(
                new CatchEffects(jumpScareManager::force, staminaService::getFoodBar, this.config.catching().slownessSeconds()),
                new CatchSwap(() -> TeamHelper.survivorsOf(teamService), random),
                new CatchSpawnTeleport(this::slenderSpawn),
                new CatchLaunch(settings.launchHeight(), new LandingDamage(this.config.catching().launchDamage())),
                this.config.catching().swapChance(),
                random,
                mood);
        this.service = new CreekService(
                this.config,
                settings,
                () -> TeamHelper.survivorsOf(teamService),
                mapProvider.getActiveInstance(),
                this::routes,
                CreakingBody::spawn,
                dread,
                witness,
                respite,
                new StagedCatchConsequence(
                        tricks,
                        reveal,
                        () -> TeamHelper.slenderOf(teamService),
                        this.config,
                        random),
                new RevealMarks(reveal, () -> TeamHelper.slenderOf(teamService), random, mood),
                mood,
                tricks::teleport,
                System::currentTimeMillis,
                random,
                sections);
        this.possession = new CreekPossession(this.service::creek, settings.sightRange(), this.config.sight().viewAngle());
        this.registerListeners();
    }

    /**
     * The creek is an entity in the world that the client draws like any other, not a camera
     * overlay, so only its own switch has a say in it.
     *
     * @return whether the creek is switched on
     */
    @Override
    public boolean enabled() {
        return this.config.enabled();
    }

    @Override
    public EventNode<Event> node() {
        return this.node;
    }

    /**
     * Hooks the creek service, which is no feature of its own, into the module's node.
     */
    private void registerListeners() {
        this.service.registerListener(this.node);
    }

    /**
     * Returns the creek as the slender possesses it.
     *
     * @return the possession target
     */
    public PossessionTarget possessionTarget() {
        return this.possession;
    }

    /**
     * Returns the creek routes of the current map.
     *
     * @return the routes, empty without a map or without routes
     */
    private List<CreekRoute> routes() {
        GameMap map = this.mapProvider.getGameMap();
        return map != null ? map.getCreekRoutes() : List.of();
    }

    /**
     * Returns the slender spawn of the current map.
     *
     * @return the spawn, {@code null} without a map or without a spawn
     */
    private @Nullable Pos slenderSpawn() {
        GameMap map = this.mapProvider.getGameMap();
        return map != null ? map.getSlenderSpawn() : null;
    }
}
