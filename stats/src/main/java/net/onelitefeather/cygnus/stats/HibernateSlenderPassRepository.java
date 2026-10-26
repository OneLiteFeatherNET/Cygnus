package net.onelitefeather.cygnus.stats;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;

/**
 * Stores the slender passes through Hibernate, on the executor the statistics use.
 * <p>
 * A redemption is one conditional update, {@code balance = balance - 1 WHERE balance > 0}, and the ledger row in
 * the same transaction, so two servers cannot spend the same pass twice. Balance changes load the row or create
 * it, then write the ledger row in the same transaction. A conflict with another server (an optimistic lock or
 * a row another server created first) rolls the transaction back and it is tried again, up to
 * {@link HibernateStatsRepository#MAX_ATTEMPTS} times. A write that still fails completes its future
 * exceptionally; nothing throws into the caller.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.21.0
 */
public final class HibernateSlenderPassRepository implements SlenderPassRepository {

    private final SessionFactory sessionFactory;
    private final Executor executor;
    private final Clock clock;

    /**
     * Creates the repository on an existing session factory.
     *
     * @param sessionFactory the session factory that knows the pass entities
     * @param executor       runs the reads and writes
     * @param clock          the time of every ledger row
     */
    public HibernateSlenderPassRepository(SessionFactory sessionFactory, Executor executor, Clock clock) {
        this.sessionFactory = sessionFactory;
        this.executor = executor;
        this.clock = clock;
    }

    /**
     * Reads the balance on the executor.
     *
     * @param player the player
     * @return the balance, {@code 0} without a row
     */
    @Override
    public CompletableFuture<Integer> balance(UUID player) {
        return read(session -> {
            SlenderPassBalance row = session.find(SlenderPassBalance.class, player);
            return row == null ? 0 : row.balance();
        });
    }

    /**
     * Spends one pass with a conditional update and writes the ledger row in the same transaction.
     *
     * @param player  the player
     * @param roundId the round
     * @return whether a pass was spent
     */
    @Override
    public CompletableFuture<Boolean> redeem(UUID player, @Nullable UUID roundId) {
        return write(session -> {
            int updated = session.createMutationQuery("update SlenderPassBalance b "
                            + "set b.balance = b.balance - 1, b.version = b.version + 1 "
                            + "where b.playerUuid = :player and b.balance > 0")
                    .setParameter("player", player)
                    .executeUpdate();
            if (updated == 0) {
                return false;
            }
            session.persist(new SlenderPassEntry(player, -1, LedgerReason.REDEEM, roundId, this.clock.instant()));
            return true;
        });
    }

    /**
     * Adds passes.
     *
     * @param player the player
     * @param amount the passes, zero or more
     * @return the adjustment
     */
    @Override
    public CompletableFuture<Adjustment> give(UUID player, int amount) {
        requireNonNegative(amount);
        return adjust(player, current -> amount, LedgerReason.ADMIN_GIVE);
    }

    /**
     * Removes passes, clamped at zero.
     *
     * @param player the player
     * @param amount the passes, zero or more
     * @return the adjustment
     */
    @Override
    public CompletableFuture<Adjustment> take(UUID player, int amount) {
        requireNonNegative(amount);
        return adjust(player, current -> -Math.min(amount, current), LedgerReason.ADMIN_TAKE);
    }

    /**
     * Sets the balance.
     *
     * @param player the player
     * @param amount the new balance, zero or more
     * @return the adjustment
     */
    @Override
    public CompletableFuture<Adjustment> set(UUID player, int amount) {
        requireNonNegative(amount);
        return adjust(player, current -> amount - current, LedgerReason.ADMIN_SET);
    }

    private CompletableFuture<Adjustment> adjust(UUID player, IntUnaryOperator deltaFor, LedgerReason reason) {
        return write(session -> {
            SlenderPassBalance row = session.find(SlenderPassBalance.class, player);
            if (row == null) {
                row = new SlenderPassBalance(player);
                session.persist(row);
            }
            int current = row.balance();
            int delta = deltaFor.applyAsInt(current);
            if (delta == 0) {
                return new Adjustment(current, 0);
            }
            row.add(delta);
            session.persist(new SlenderPassEntry(player, delta, reason, null, this.clock.instant()));
            return new Adjustment(current + delta, delta);
        });
    }

    private <T> CompletableFuture<T> read(Function<Session, T> query) {
        CompletableFuture<T> result = new CompletableFuture<>();
        try {
            this.executor.execute(() -> {
                try {
                    result.complete(this.sessionFactory.fromSession(query));
                } catch (RuntimeException exception) {
                    result.completeExceptionally(exception);
                }
            });
        } catch (RejectedExecutionException exception) {
            result.completeExceptionally(exception);
        }
        return result;
    }

    private <T> CompletableFuture<T> write(Function<Session, T> task) {
        CompletableFuture<T> result = new CompletableFuture<>();
        try {
            this.executor.execute(() -> {
                try {
                    result.complete(attempt(task));
                } catch (RuntimeException exception) {
                    result.completeExceptionally(exception);
                }
            });
        } catch (RejectedExecutionException exception) {
            result.completeExceptionally(exception);
        }
        return result;
    }

    private <T> T attempt(Function<Session, T> task) {
        for (int attempt = 1; ; attempt++) {
            try {
                return this.sessionFactory.fromTransaction(task::apply);
            } catch (RuntimeException exception) {
                if (attempt < HibernateStatsRepository.MAX_ATTEMPTS && HibernateStatsRepository.conflict(exception)) {
                    continue;
                }
                throw exception;
            }
        }
    }

    private static void requireNonNegative(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("A pass amount cannot be negative: " + amount);
        }
    }
}
