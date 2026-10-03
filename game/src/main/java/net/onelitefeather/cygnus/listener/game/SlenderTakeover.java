package net.onelitefeather.cygnus.listener.game;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.sound.SoundEvent;
import net.onelitefeather.cygnus.GameFeature;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.SlenderReviveEvent;
import net.onelitefeather.cygnus.phase.GamePhase;
import net.onelitefeather.cygnus.utils.RepeatingTask;
import net.theevilreaper.aves.util.Players;
import net.theevilreaper.xerus.api.phase.Phase;
import net.theevilreaper.xerus.api.team.Team;
import net.theevilreaper.xerus.api.team.TeamService;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Supplier;

import static net.onelitefeather.cygnus.common.config.GameConfig.SLENDER_KEY;
import static net.onelitefeather.cygnus.common.config.GameConfig.SURVIVOR_KEY;

/**
 * Hands the slender's role to a survivor after the slender left the round.
 * <p>
 * The chosen survivor is told first: they go blind and a countdown runs on their screen, and only
 * once it is over do they switch sides. Until then they are still a survivor. The blindness lasts a
 * second longer than the countdown, so it covers the teleport to the slender's spawn.
 * </p>
 * <p>
 * If the chosen survivor dies or leaves during the countdown, another one is chosen by the same
 * rules; without anyone left to take over, the round ends.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.16.0
 */
public final class SlenderTakeover implements GameFeature {

    /** How long the chosen survivor is warned before they switch sides, in seconds. */
    static final int COUNTDOWN_SECONDS = 3;

    /** The least time a round must have left for a new slender to be worth it, in seconds. */
    private static final int MIN_SECONDS_LEFT = 120;

    /** The one who takes over and at least one left to hunt. */
    private static final int MIN_SURVIVORS = 2;

    /**
     * How often per round a survivor may take over. If the new slender leaves as well, the round
     * ends: by then it has lost too much to be worth saving.
     */
    private static final int MAX_TAKEOVERS = 1;

    /** The countdown plus a second to cover the teleport, in ticks. */
    private static final int BLINDNESS_TICKS = (COUNTDOWN_SECONDS + 1) * 20;

    private static final Sound COUNTDOWN_SOUND =
            Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_BASEDRUM, Sound.Source.MASTER, 1F, 0.5F);

    private static final Title.Times COUNTDOWN_TIMES =
            Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO);

    private final EventNode<Event> node = EventNode.all("slender-takeover");

    private final TeamService teamService;
    private final Supplier<Phase> phaseSupplier;
    private final RepeatingTask task = new RepeatingTask(this::tick);
    private int takeovers;
    private @Nullable Player chosen;
    private int secondsLeft;

    /**
     * Creates the takeover.
     *
     * @param teamService   the teams of the round
     * @param phaseSupplier supplies the current phase, a takeover only runs during the {@link GamePhase}
     */
    public SlenderTakeover(TeamService teamService, Supplier<Phase> phaseSupplier) {
        this.teamService = teamService;
        this.phaseSupplier = phaseSupplier;
        this.registerListeners();
    }

    @Override
    public EventNode<Event> node() {
        return this.node;
    }

    /**
     * Follows the chosen survivor and the end of the round.
     */
    private void registerListeners() {
        this.node.addListener(PlayerDeathEvent.class, event -> this.lost(event.getPlayer()));
        this.node.addListener(PlayerDisconnectEvent.class, event -> this.lost(event.getPlayer()));
        this.node.addListener(GameFinishEvent.class, _ -> this.cancel());
    }

    /**
     * Chooses a survivor to take over and starts the countdown, or ends the round if nobody can.
     *
     * @param gamePhase the running round
     */
    public void begin(GamePhase gamePhase) {
        Team survivorTeam = this.team(SURVIVOR_KEY);
        boolean canTakeOver = this.takeovers < MAX_TAKEOVERS
                && gamePhase.getCurrentTicks() >= MIN_SECONDS_LEFT
                && survivorTeam.getCurrentSize() >= MIN_SURVIVORS;
        Optional<Player> pick = canTakeOver
                ? Players.getRandomPlayer(new ArrayList<>(survivorTeam.getPlayers()))
                : Optional.empty();
        if (pick.isEmpty()) {
            this.end(gamePhase);
            return;
        }

        ++this.takeovers;
        Player player = pick.get();
        this.chosen = player;
        this.secondsLeft = COUNTDOWN_SECONDS;
        player.addEffect(new Potion(PotionEffect.BLINDNESS, 0, BLINDNESS_TICKS));
        this.announce(player);
        this.task.start(1, ChronoUnit.SECONDS);
    }

    /**
     * Counts one second down and switches the chosen survivor over once the countdown is over.
     */
    void tick() {
        Player player = this.chosen;
        if (player == null) return;
        if (--this.secondsLeft > 0) {
            this.announce(player);
            return;
        }

        this.task.stop();
        this.chosen = null;
        Team survivorTeam = this.team(SURVIVOR_KEY);
        // Someone may have died during the countdown: without anyone left to hunt, the round is over
        if (survivorTeam.getCurrentSize() < MIN_SURVIVORS) {
            if (this.phaseSupplier.get() instanceof GamePhase gamePhase) {
                this.end(gamePhase);
            }
            return;
        }
        survivorTeam.removePlayer(player);
        this.team(SLENDER_KEY).addPlayer(player);
        EventDispatcher.call(new SlenderReviveEvent(player));
    }

    /**
     * Returns the survivor who is about to take over.
     *
     * @return the chosen survivor, {@code null} while no countdown runs
     */
    @Nullable Player chosen() {
        return this.chosen;
    }

    /**
     * Shows the chosen survivor how long they have left.
     *
     * @param player the chosen survivor
     */
    private void announce(Player player) {
        player.showTitle(Title.title(
                Messages.SLENDER_TAKEOVER_TITLE,
                Component.text(this.secondsLeft, NamedTextColor.RED),
                COUNTDOWN_TIMES));
        player.playSound(COUNTDOWN_SOUND, Sound.Emitter.self());
    }

    /**
     * Chooses again once the chosen survivor died or left. The lost attempt does not count.
     *
     * @param player the player who died or left
     */
    private void lost(Player player) {
        if (player != this.chosen) return;
        this.task.stop();
        this.chosen = null;
        --this.takeovers;
        if (this.phaseSupplier.get() instanceof GamePhase gamePhase) {
            this.begin(gamePhase);
        }
    }

    /**
     * Ends the round because the slender left and nobody can take over.
     *
     * @param gamePhase the running round
     */
    private void end(GamePhase gamePhase) {
        gamePhase.setFinishEvent(new GameFinishEvent(GameFinishEvent.Reason.SLENDER_LEFT));
        gamePhase.finish();
    }

    /**
     * Calls a running countdown off.
     */
    private void cancel() {
        this.task.stop();
        this.chosen = null;
    }

    private Team team(Key key) {
        return this.teamService.getTeam(key)
                .orElseThrow(() -> new IllegalStateException("Team " + key.asString() + " not found"));
    }
}
