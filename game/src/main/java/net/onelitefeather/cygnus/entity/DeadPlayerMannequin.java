package net.onelitefeather.cygnus.entity;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityPose;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.entity.metadata.avatar.MannequinMeta;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import net.minestom.server.network.player.ResolvableProfile;
import net.minestom.server.particle.Particle;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Custom mannequin entity representing a deceased survivor player.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.7.0
 */
public class DeadPlayerMannequin extends LivingEntity {

    private final UUID originalPlayerUuid;
    private final @Nullable PlayerSkin skin;
    private long ticksAlive = 0;
    private boolean decayed = false;

    /**
     * Creates a sleeping corpse impersonating the given player. Call {@link #setInstance} on
     * the result to actually spawn it.
     *
     * @param player the player this corpse represents
     */
    public static DeadPlayerMannequin sleeping(Player player) {
        return sleeping(player.getUuid(), player.getSkin());
    }

    /**
     * Creates a sleeping corpse impersonating a player identity. Call {@link #setInstance} on
     * the result to actually spawn it.
     *
     * @param originalPlayerUuid the UUID of the player this corpse represents
     * @param skin               the skin to render the corpse with, or null for the default skin
     */
    public static DeadPlayerMannequin sleeping(UUID originalPlayerUuid, @Nullable PlayerSkin skin) {
        return new DeadPlayerMannequin(originalPlayerUuid, skin, EntityPose.SLEEPING);
    }

    /**
     * Creates a standing, packet-only JumpScare phantom for a player identity. Never call
     * {@link #setInstance} on the result, it only exists as raw packets sent to one victim.
     *
     * @param originalPlayerUuid the UUID of the player this phantom impersonates
     * @param skin               the skin to render the phantom with, or null for the default skin
     * @param position           the position/rotation to spawn the phantom at
     */
    public static DeadPlayerMannequin standing(UUID originalPlayerUuid, @Nullable PlayerSkin skin, Pos position) {
        DeadPlayerMannequin phantom = new DeadPlayerMannequin(originalPlayerUuid, skin, EntityPose.STANDING);
        phantom.setPacketPosition(position);
        return phantom;
    }

    /**
     * Creates a new entity with the given values.
     *
     * @param originalPlayerUuid of the player
     * @param skin               of the player
     * @param initialPose        for the entity
     */
    private DeadPlayerMannequin(UUID originalPlayerUuid, @Nullable PlayerSkin skin, EntityPose initialPose) {
        super(EntityType.MANNEQUIN);
        this.originalPlayerUuid = originalPlayerUuid;
        this.skin = skin;

        setNoGravity(true);
        setHasPhysics(false);
        setPose(initialPose);

        if (getEntityMeta() instanceof MannequinMeta mannequinMeta) {
            mannequinMeta.setDescription(null);
            mannequinMeta.setImmovable(true);
            mannequinMeta.setCapeEnabled(false);
            if (skin != null) {
                mannequinMeta.setProfile(new ResolvableProfile(skin));
            }
        }
    }

    @Override
    public void update(long time) {
        if (getInstance() == null || isRemoved()) return;

        ticksAlive++;

        // Decay logic: after 2 minutes (2400 ticks), swap head to skeleton skull
        if (!decayed && ticksAlive >= 2400) {
            decayed = true;
            setEquipment(EquipmentSlot.HELMET, ItemStack.of(Material.SKELETON_SKULL));
        }

        // Atmosphere particles every 5 seconds (100 ticks) centered over the body
        if (ticksAlive % 100 == 0) {
            Pos pos = getPosition();
            getInstance().sendGroupedPacket(
                    new ParticlePacket(Particle.SPORE_BLOSSOM_AIR, true, false, pos.x(), pos.y() + 0.3, pos.z(), 0.4f, 0.2f, 0.4f, 0.01f, 15)
            );
        }
    }

    /**
     * Sets the position/rotation for a mannequin that is never added to an instance.
     *
     * @param pos the position/rotation to spawn the mannequin at
     */
    public void setPacketPosition(Pos pos) {
        setPositionInternal(pos, pos.yaw());
    }

    /**
     * Builds the spawn packet for a mannequin that is never added to an instance.
     *
     * @return the spawn packet for this mannequin
     */
    public SpawnEntityPacket toSpawnPacket() {
        return getSpawnPacket();
    }

    /**
     * Gets the UUID of the player this mannequin impersonates.
     *
     * @return the original player UUID
     */
    public UUID getOriginalPlayerUuid() {
        return originalPlayerUuid;
    }

    /**
     * Gets the skin this mannequin is rendered with.
     *
     * @return the skin, or null for the default skin
     */
    public @Nullable PlayerSkin getSkin() {
        return skin;
    }
}
