package net.onelitefeather.cygnus.footprint;

import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.BlockChangePacket;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * A print that only exists on the clients of its viewers.
 * <p>
 * It is a fake petal block sent with a block change packet. The server keeps the real block, which
 * is air, so hiding the print means sending that block again. The print does not tick on its own,
 * the spawner calls {@link #second()} once per second.
 * </p>
 *
 * @author Joltra
 * @version 1.0.0
 * @since 2.16.0
 */
final class Footprint {

    private final Instance instance;
    private final BlockVec block;
    private final FootprintKind kind;
    private final String facing;
    private final int lifetimeSeconds;
    private final int fadeAfterSeconds;
    private final Predicate<Player> viewers;
    private final Set<Player> shownTo;
    private int ageSeconds;
    private volatile boolean faded;

    /**
     * Creates a print. Nobody sees it before {@link #refresh()}.
     *
     * @param instance         the instance it lies in
     * @param block            the air block it fills
     * @param kind             whose print it is
     * @param facing           how the model is turned, one of {@link FootprintSpawner#FACINGS}
     * @param lifetimeSeconds  how long it stays
     * @param fadeAfterSeconds after how many seconds it shows the faded stage
     * @param viewers          who may see it
     */
    Footprint(Instance instance, BlockVec block, FootprintKind kind, String facing, int lifetimeSeconds,
              int fadeAfterSeconds, Predicate<Player> viewers) {
        this.instance = instance;
        this.block = block;
        this.kind = kind;
        this.facing = facing;
        this.lifetimeSeconds = lifetimeSeconds;
        this.fadeAfterSeconds = fadeAfterSeconds;
        this.viewers = viewers;
        this.shownTo = ConcurrentHashMap.newKeySet();
    }

    /**
     * Checks every player of the instance against the rule. New matches get the print, players
     * who no longer match get the real block back.
     */
    void refresh() {
        for (Player player : this.instance.getPlayers()) {
            if (this.viewers.test(player)) {
                if (this.shownTo.add(player)) this.send(player);
            } else if (this.shownTo.remove(player)) {
                this.hide(player);
            }
        }
    }

    /**
     * Sends the print again to a player whose client just got the chunk, which replaced it.
     *
     * @param player the player
     */
    void resend(Player player) {
        if (player.getInstance() != this.instance || !this.viewers.test(player)) return;
        this.shownTo.add(player);
        this.send(player);
    }

    /**
     * Drops a player who left, without sending anything.
     *
     * @param player the player
     */
    void forget(Player player) {
        this.shownTo.remove(player);
    }

    /**
     * Ages the print by one second. It switches to the faded stage once, and at the end of its
     * lifetime it gives every viewer the real block back.
     *
     * @return {@code true} if the print is gone now
     */
    boolean second() {
        this.ageSeconds++;
        if (this.ageSeconds >= this.lifetimeSeconds) {
            this.remove();
            return true;
        }
        if (!this.faded && this.ageSeconds >= this.fadeAfterSeconds) {
            this.faded = true;
            this.shownTo.forEach(this::send);
        }
        return false;
    }

    /**
     * Gives every viewer the real block back.
     */
    void remove() {
        for (Player player : this.shownTo) {
            this.hide(player);
        }
        this.shownTo.clear();
    }

    /**
     * Returns whether a player has the print on their client.
     *
     * @param player the player
     * @return {@code true} if the print was sent to them
     */
    boolean isShownTo(Player player) {
        return this.shownTo.contains(player);
    }

    /**
     * Returns the block the print fills.
     *
     * @return the block position
     */
    BlockVec block() {
        return this.block;
    }

    /**
     * Returns whether the print shows its faded stage.
     *
     * @return {@code true} once faded
     */
    boolean faded() {
        return this.faded;
    }

    /**
     * Returns the petal state the print shows right now.
     *
     * @return the block state
     */
    Block state() {
        return this.kind.block(this.facing, this.faded);
    }

    private void send(Player player) {
        player.sendPacket(new BlockChangePacket(this.block, this.state()));
    }

    private void hide(Player player) {
        player.sendPacket(new BlockChangePacket(this.block, this.instance.getBlock(this.block)));
    }
}
