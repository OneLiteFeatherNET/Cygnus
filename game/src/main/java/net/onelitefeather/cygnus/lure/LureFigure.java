package net.onelitefeather.cygnus.lure;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket;
import net.minestom.server.network.packet.server.play.EntityHeadLookPacket;
import net.onelitefeather.cygnus.entity.DeadPlayerMannequin;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The dead teammate standing in the distance. It only exists as packets sent to the lured survivor,
 * so nobody else sees it or bumps into it.
 * <p>
 * It takes the dead survivor's skin and nothing else. Living survivors never see each other's name
 * tags, so a name above its head would give it away.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public final class LureFigure {

    private final Player survivor;
    private final DeadPlayerMannequin body;
    private final UUID identity;
    private final @Nullable PlayerSkin skin;
    private final Pos position;
    private boolean removed;

    private LureFigure(Player survivor, DeadPlayerMannequin body, UUID identity, @Nullable PlayerSkin skin, Pos position) {
        this.survivor = survivor;
        this.body = body;
        this.identity = identity;
        this.skin = skin;
        this.position = position;
    }

    /**
     * Puts a figure of a dead survivor in front of one survivor's eyes only.
     *
     * @param survivor who sees it
     * @param corpse   the dead survivor it impersonates
     * @param at       where it stands and where it looks
     * @return the figure
     */
    public static LureFigure show(Player survivor, DeadPlayerMannequin corpse, Pos at) {
        DeadPlayerMannequin body = DeadPlayerMannequin.standing(corpse.getOriginalPlayerUuid(), corpse.getSkin(), at);
        survivor.sendPacket(body.toSpawnPacket());
        survivor.sendPacket(body.getMetadataPacket());
        survivor.sendPacket(new EntityHeadLookPacket(body.getEntityId(), at.yaw()));
        return new LureFigure(survivor, body, corpse.getOriginalPlayerUuid(), corpse.getSkin(), at);
    }

    /**
     * Tells whom the figure impersonates.
     *
     * @return the dead survivor's id
     */
    public UUID identity() {
        return this.identity;
    }

    /**
     * Tells which skin the figure wears.
     *
     * @return the skin, or {@code null} for the default skin
     */
    public @Nullable PlayerSkin skin() {
        return this.skin;
    }

    /**
     * Tells where the figure stands.
     *
     * @return its feet, looking at the survivor
     */
    public Pos position() {
        return this.position;
    }

    /**
     * Takes the figure away. Calling it again does nothing.
     */
    public void remove() {
        if (this.removed) return;
        this.removed = true;
        if (this.survivor.isOnline()) {
            this.survivor.sendPacket(new DestroyEntitiesPacket(this.body.getEntityId()));
        }
    }
}
