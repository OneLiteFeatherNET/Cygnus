package net.onelitefeather.cygnus.creek;

import net.onelitefeather.cygnus.telemetry.TickSections;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.onelitefeather.cygnus.GameFeature;
import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.common.creek.CreekRoute;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.creek.body.CreakingBody;
import net.onelitefeather.cygnus.creek.consequence.CatchEffects;
import net.onelitefeather.cygnus.creek.consequence.CatchLaunch;
import net.onelitefeather.cygnus.creek.consequence.CatchSwap;
import net.onelitefeather.cygnus.creek.consequence.CatchTricks;
import net.onelitefeather.cygnus.creek.consequence.GlowReveal;
import net.onelitefeather.cygnus.creek.consequence.LandingDamage;
import net.onelitefeather.cygnus.creek.consequence.StagedCatchConsequence;
import net.onelitefeather.cygnus.creek.dread.CreekWitness;
import net.onelitefeather.cygnus.creek.dread.DreadSource;
import net.onelitefeather.cygnus.jumpscare.JumpScareManager;
import net.onelitefeather.cygnus.map.GameMapProvider;
import net.onelitefeather.cygnus.possession.PossessionTarget;
import net.onelitefeather.cygnus.stamina.StaminaService;
import net.onelitefeather.cygnus.team.TeamHelper;
import net.theevilreaper.xerus.api.team.TeamService;

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

    /**
     * Builds the creek, with its step measured for the slow tick report.
     *
     * @param config           the creek settings
     * @param teamService      the teams of the round
     * @param mapProvider      the maps and the active instance
     * @param dread            rates how scared each survivor is
     * @param witness          hears about catches and sightings
     * @param jumpScareManager plays the jump scare of a catch
     * @param staminaService   the stamina bars a catch drains
     * @param sections         measures how long each step takes
     * @since 2.15.0
     */
    public CreekModule(CreekConfig config, TeamService teamService,
                       GameMapProvider mapProvider, DreadSource dread, CreekWitness witness,
                       JumpScareManager jumpScareManager, StaminaService staminaService, TickSections sections) {
        this.config = config;
        this.mapProvider = mapProvider;
        // Every step and every catch runs on the scheduler thread, but Random is thread-safe
        // anyway, which keeps a stray call from elsewhere harmless.
        Random random = new Random();
        this.service = new CreekService(
                this.config,
                () -> TeamHelper.survivorsOf(teamService),
                mapProvider.getActiveInstance(),
                this::routes,
                CreakingBody::spawn,
                dread,
                witness,
                new StagedCatchConsequence(
                        new CatchTricks(
                                new CatchEffects(jumpScareManager::force, staminaService::getFoodBar, this.config.catching().slownessSeconds()),
                                new CatchSwap(() -> TeamHelper.survivorsOf(teamService), random),
                                new CatchLaunch(this.config.catching().launchHeight(), new LandingDamage(this.config.catching().launchDamage())),
                                this.config.catching().swapChance(),
                                random),
                        new GlowReveal(this.config.catching().betrayalGlowSeconds(), sections),
                        () -> TeamHelper.slenderOf(teamService),
                        this.config,
                        random),
                System::currentTimeMillis,
                random,
                sections);
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
        return this.service;
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
}
