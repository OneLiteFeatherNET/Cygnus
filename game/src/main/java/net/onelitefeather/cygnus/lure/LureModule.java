package net.onelitefeather.cygnus.lure;

import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.onelitefeather.cygnus.GameFeature;
import net.onelitefeather.cygnus.common.config.LureConfig;
import net.onelitefeather.cygnus.creek.CreekRespite;
import net.onelitefeather.cygnus.creek.consequence.LocatorReveal;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.jumpscare.JumpScareManager;
import net.onelitefeather.cygnus.map.GameMapProvider;
import net.onelitefeather.cygnus.sanity.SanityService;
import net.onelitefeather.cygnus.team.TeamHelper;
import net.onelitefeather.cygnus.telemetry.TickSections;
import net.theevilreaper.xerus.api.team.TeamService;

import java.util.Random;

/**
 * Puts the lure into every round: it starts with the round and stops when the round ends.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public final class LureModule implements GameFeature {

    private final EventNode<Event> node = EventNode.all("lure");
    private final LureConfig config;
    private final LureService service;

    /**
     * Sets up the lure for the rounds on the given maps.
     *
     * @param config      the settings
     * @param teamService the teams of the round
     * @param mapProvider the maps and the active instance
     * @param jumpScares  holds the corpses and plays the scare
     * @param respite     the breather from the creek, shared with it
     * @param sanity      the survivors' fear
     * @param sections    measures how long each step takes
     */
    public LureModule(LureConfig config, TeamService teamService, GameMapProvider mapProvider,
                      JumpScareManager jumpScares, CreekRespite respite, SanityService sanity, TickSections sections) {
        // One random source for the rolls, the spots and the follow-ups.
        Random random = new Random();
        this(config, new LureService(
                config,
                () -> TeamHelper.survivorsOf(teamService).stream().filter(survivor -> !survivor.isDead()).toList(),
                jumpScares,
                new LureSpotFinder(mapProvider.getActiveInstance()),
                new LureAftermath(
                        config,
                        sanity::lured,
                        respite,
                        () -> TeamHelper.slenderOf(teamService),
                        new LocatorReveal(config.effectSeconds(), sections),
                        () -> LureOutcome.roll(random.nextDouble())),
                System::currentTimeMillis,
                random,
                sections));
    }

    /**
     * Wires an already built service into the round.
     *
     * @param config  the settings
     * @param service the lure
     */
    LureModule(LureConfig config, LureService service) {
        this.config = config;
        this.service = service;
        this.node.addListener(GameStartEvent.class, _ -> this.service.start());
        this.node.addListener(GameFinishEvent.class, _ -> this.service.stop());
    }

    /**
     * The figure is a packet sent to one survivor, not a camera overlay, so only its own switch has
     * a say in it.
     *
     * @return whether the lure is switched on
     */
    @Override
    public boolean enabled() {
        return this.config.enabled();
    }

    @Override
    public EventNode<Event> node() {
        return this.node;
    }
}
