package net.onelitefeather.cygnus.pass;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.stats.SlenderPassRepository;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Lets a player with a slender pass reserve the Slender for the next round, and spends the pass when that round
 * starts.
 * <p>
 * There is one reservation at a time, held in memory by this server. A reservation reads the balance off the tick
 * thread; the decision runs on the tick thread when the read returns. At the round start the holder who is online
 * gets the role at once, and the pass is spent in the background with a conditional update. When that spend fails
 * (the balance was taken away in the meantime, or the database failed), the holder keeps the role for the round and
 * the failure is reported through {@link DebitFailureListener}. A reservation ends when its holder cancels, leaves
 * or is offline at the round start.
 * </p>
 * <p>
 * Every field is read and written on the tick thread, except what the repository completes on its own executor,
 * which only reaches the tick thread through the executor given here.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.21.0
 */
public final class SlenderPassService {

    /**
     * Receives a spend that failed at the round start. The holder keeps the role for that round.
     */
    @FunctionalInterface
    public interface DebitFailureListener {

        /**
         * Reports the failed spend.
         *
         * @param player  the holder who keeps the role
         * @param roundId the round the spend was for, {@code null} when the round is not known
         * @param cause   the database error, or {@code null} when the balance was already zero
         */
        void debitFailed(UUID player, @Nullable UUID roundId, @Nullable Throwable cause);
    }

    private final SlenderPassRepository repository;
    private final Executor tick;
    private final Function<UUID, @Nullable Player> online;
    private final BooleanSupplier lobbyOpen;
    private final PassFeedback feedback;
    private final DebitFailureListener debitFailures;
    private @Nullable UUID holder;

    /**
     * Creates the service.
     *
     * @param repository    stores the balances and the ledger
     * @param tick          applies the results of the repository on the tick thread
     * @param online        finds an online player by UUID, {@code null} when offline
     * @param lobbyOpen     tells whether the lobby phase is running
     * @param feedback      delivers the messages and the item balance
     * @param debitFailures receives a spend that failed at the round start
     */
    public SlenderPassService(SlenderPassRepository repository, Executor tick, Function<UUID, @Nullable Player> online,
                               BooleanSupplier lobbyOpen, PassFeedback feedback, DebitFailureListener debitFailures) {
        this.repository = repository;
        this.tick = tick;
        this.online = online;
        this.lobbyOpen = lobbyOpen;
        this.feedback = feedback;
        this.debitFailures = debitFailures;
    }

    /**
     * Tells whether the feature is available, that is whether passes are stored.
     *
     * @return {@code true} when passes are stored
     */
    public boolean available() {
        return this.repository.available();
    }

    /**
     * Returns the holder of the current reservation.
     *
     * @return the holder, empty when nobody holds it
     */
    public Optional<UUID> reservedBy() {
        return Optional.ofNullable(this.holder);
    }

    /**
     * Shows the balance on the lobby item of a player who joins the lobby.
     *
     * @param player the player
     */
    public void onLobbyJoin(Player player) {
        if (!available()) {
            return;
        }
        UUID uuid = player.getUuid();
        this.repository.balance(uuid)
                .thenAcceptAsync(balance -> showIfLobby(uuid, balance), this.tick)
                .exceptionallyAsync(error -> unavailable(uuid), this.tick);
    }

    /**
     * The lobby item: reserves for a player who holds no reservation, cancels for the holder.
     *
     * @param player the player who used the item
     */
    public void toggle(Player player) {
        if (player.getUuid().equals(this.holder)) {
            cancel(player);
        } else {
            reserve(player);
        }
    }

    /**
     * Reserves the Slender for the next round. Refused outside the lobby, while another player holds the
     * reservation, and without a pass. The balance is read first, the decision follows on the tick thread.
     *
     * @param player the player
     */
    public void reserve(Player player) {
        UUID uuid = player.getUuid();
        if (!available()) {
            this.feedback.tell(uuid, Messages.PASS_UNAVAILABLE);
            return;
        }
        if (!this.lobbyOpen.getAsBoolean()) {
            this.feedback.tell(uuid, Messages.PASS_CLOSED);
            return;
        }
        if (uuid.equals(this.holder)) {
            this.feedback.tell(uuid, Messages.PASS_ALREADY_RESERVED);
            return;
        }
        if (this.holder != null) {
            this.feedback.tell(uuid, Messages.PASS_TAKEN);
            return;
        }
        this.repository.balance(uuid)
                .thenAcceptAsync(balance -> decideReservation(uuid, balance), this.tick)
                .exceptionallyAsync(error -> unavailable(uuid), this.tick);
    }

    /**
     * Cancels the reservation of a player.
     *
     * @param player the player
     */
    public void cancel(Player player) {
        UUID uuid = player.getUuid();
        if (!available()) {
            this.feedback.tell(uuid, Messages.PASS_UNAVAILABLE);
            return;
        }
        if (!uuid.equals(this.holder)) {
            this.feedback.tell(uuid, Messages.PASS_NOTHING_TO_CANCEL);
            return;
        }
        this.holder = null;
        this.feedback.tell(uuid, Messages.PASS_CANCELLED);
    }

    /**
     * Tells a player how many passes they have, and refreshes the lobby item.
     *
     * @param player the player
     */
    public void balance(Player player) {
        UUID uuid = player.getUuid();
        if (!available()) {
            this.feedback.tell(uuid, Messages.PASS_UNAVAILABLE);
            return;
        }
        this.repository.balance(uuid)
                .thenAcceptAsync(balance -> {
                    this.feedback.tell(uuid, Messages.passBalance(balance));
                    showIfLobby(uuid, balance);
                }, this.tick)
                .exceptionallyAsync(error -> unavailable(uuid), this.tick);
    }

    /**
     * Drops the reservation of a player who disconnects or leaves the lobby. Nothing is announced.
     *
     * @param player the player
     */
    public void release(UUID player) {
        if (player.equals(this.holder)) {
            this.holder = null;
        }
    }

    /**
     * Takes the reservation for the round that starts. The reservation ends here, whatever the outcome.
     * <p>
     * A holder who is offline loses the reservation and nothing is spent. A holder who is online gets the
     * returned player as the Slender, and the pass is spent in the background.
     * </p>
     *
     * @param roundId the round that starts, {@code null} when the round is not known
     * @return the holder who plays the Slender, or {@code null} to draw the Slender at random
     */
    public @Nullable Player claimRound(@Nullable UUID roundId) {
        UUID claimed = this.holder;
        this.holder = null;
        if (claimed == null) {
            return null;
        }
        Player player = this.online.apply(claimed);
        if (player == null) {
            return null;
        }
        this.repository.redeem(claimed, roundId).whenComplete((spent, error) -> {
            if (error != null) {
                this.debitFailures.debitFailed(claimed, roundId, error);
            } else if (!spent) {
                this.debitFailures.debitFailed(claimed, roundId, null);
            }
        });
        return player;
    }

    /**
     * Adds passes to a balance. The result is handed to the reply on the tick thread.
     *
     * @param target the player
     * @param amount the passes, zero or more
     * @param reply  receives the new balance
     */
    public void give(UUID target, int amount, Consumer<SlenderPassRepository.Adjustment> reply) {
        this.repository.give(target, amount).thenAcceptAsync(reply, this.tick);
    }

    /**
     * Removes passes from a balance, never below zero. The result is handed to the reply on the tick thread.
     *
     * @param target the player
     * @param amount the passes, zero or more
     * @param reply  receives the new balance and the delta
     */
    public void take(UUID target, int amount, Consumer<SlenderPassRepository.Adjustment> reply) {
        this.repository.take(target, amount).thenAcceptAsync(reply, this.tick);
    }

    /**
     * Sets a balance. The result is handed to the reply on the tick thread.
     *
     * @param target the player
     * @param amount the new balance, zero or more
     * @param reply  receives the new balance and the delta
     */
    public void set(UUID target, int amount, Consumer<SlenderPassRepository.Adjustment> reply) {
        this.repository.set(target, amount).thenAcceptAsync(reply, this.tick);
    }

    private void decideReservation(UUID uuid, int balance) {
        if (!this.lobbyOpen.getAsBoolean()) {
            this.feedback.tell(uuid, Messages.PASS_CLOSED);
            return;
        }
        if (uuid.equals(this.holder)) {
            this.feedback.tell(uuid, Messages.PASS_ALREADY_RESERVED);
            return;
        }
        if (this.holder != null) {
            this.feedback.tell(uuid, Messages.PASS_TAKEN);
            return;
        }
        if (balance <= 0) {
            this.feedback.tell(uuid, Messages.PASS_NO_BALANCE);
            return;
        }
        this.holder = uuid;
        this.feedback.tell(uuid, Messages.PASS_RESERVED);
        this.feedback.tellLobby(Messages.PASS_REDEEMED, uuid);
        this.feedback.showBalance(uuid, balance);
    }

    private void showIfLobby(UUID uuid, int balance) {
        if (this.lobbyOpen.getAsBoolean()) {
            this.feedback.showBalance(uuid, balance);
        }
    }

    private @Nullable Void unavailable(UUID uuid) {
        this.feedback.tell(uuid, Messages.PASS_UNAVAILABLE);
        return null;
    }
}
