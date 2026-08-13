package net.onelitefeather.cygnus.player;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.network.packet.server.play.EntityAttributesPacket;
import net.minestom.server.network.packet.server.play.WorldBorderWarningReachPacket;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.server.sound.SoundEvent;
import net.onelitefeather.cygnus.common.player.InstanceSwitchChunkPlayer;

import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings("java:S3252")
public final class CygnusPlayer extends InstanceSwitchChunkPlayer {

    static final AttributeModifier SPEED_MODIFIER_SPRINTING =
            new AttributeModifier(Key.key("cygnus:sprinting"), 0.25, AttributeOperation.ADD_MULTIPLIED_TOTAL);

    static final AttributeModifier DISABLED_SPRINT_MODIFIER =
            new AttributeModifier(Key.key("cygnus:sprinting"), 0.0, AttributeOperation.ADD_MULTIPLIED_TOTAL);

    private static final float HEALTH_THRESHOLD = 6.0f; // 3 hearts
    private static final int MAX_INTERVAL_TICKS = 36;   // Every 1.8s (slow, subtle pulse at start)
    private static final int MIN_INTERVAL_TICKS = 12;   // Every 0.6s (fast & tense without sound overlapping)

    private boolean blockedSprinting;
    private int heartbeatTicks;
    private boolean heartbeatActive;

    public CygnusPlayer(PlayerConnection playerConnection, GameProfile gameProfile) {
        super(playerConnection, gameProfile);
        this.blockedSprinting = false;
        this.heartbeatTicks = 0;
        this.heartbeatActive = false;
    }

    /**
     * Sets if the player is blocked from sprinting.
     *
     * @param blockedSprinting {@code true} if the player is blocked from sprinting, otherwise {@code false}.
     */
    public void setBlockedSprinting(boolean blockedSprinting) {
        this.blockedSprinting = blockedSprinting;
    }

    /**
     * Sets if the entity is sprinting.
     *
     * @param sprinting true to make the entity sprint otherwise false for no sprinting
     */
    @Override
    public void setSprinting(boolean sprinting) {
        if (blockedSprinting) {
            this.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(SPEED_MODIFIER_SPRINTING);
            this.getAttribute(Attribute.MOVEMENT_SPEED).addModifier(DISABLED_SPRINT_MODIFIER);
            this.entityMeta.setSprinting(false);
            this.sendSpringPackets();
            return;
        }

        if (sprinting) {
            this.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(DISABLED_SPRINT_MODIFIER);
            this.getAttribute(Attribute.MOVEMENT_SPEED).addModifier(SPEED_MODIFIER_SPRINTING);
        } else {
            this.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(SPEED_MODIFIER_SPRINTING);
            this.getAttribute(Attribute.MOVEMENT_SPEED).addModifier(DISABLED_SPRINT_MODIFIER);
        }
        this.entityMeta.setSprinting(sprinting);
        this.sendSpringPackets();
    }

    /**
     * Sends the packets to the player to update the sprinting state.
     */
    public void sendSpringPackets() {
        sendPacket(getPropertiesPacket());
        sendPacket(getMetadataPacket());
    }

    /**
     * Checks if the player has blocked sprinting.
     *
     * @return {@code true} if the player has blocked sprinting, otherwise {@code false}.
     */
    public boolean hasBlockedSprinting() {
        return blockedSprinting;
    }

    /**
     * Updates the heartbeat sound and red border vignette effect on player tick.
     */
    public void tickHeartbeat() {
        float health = getHealth();

        if (health > HEALTH_THRESHOLD || health <= 0 || isDead()) {
            if (heartbeatActive) {
                resetHeartbeat();
            }
            return;
        }

        heartbeatActive = true;

        float intensity = Math.clamp(1.0f - (health / HEALTH_THRESHOLD), 0.0f, 1.0f);

        double distanceToBorder = 29_999_984.0;
        var instance = getInstance();
        if (instance != null && instance.getWorldBorder() != null) {
            var border = instance.getWorldBorder();
            double radius = border.diameter() / 2.0;
            double dx = Math.abs(getPosition().x() - border.centerX());
            double dz = Math.abs(getPosition().z() - border.centerZ());
            distanceToBorder = Math.max(1.0, radius - Math.max(dx, dz));
        }

        // Non-linear visual curve makes the red border vignette stronger earlier and very intense at low HP
        float visualIntensity = (float) Math.pow(intensity, 0.6);
        float clampedIntensity = Math.min(visualIntensity, 0.995f);
        int warningBlocks = (int) (distanceToBorder / (1.0f - clampedIntensity));
        sendPacket(new WorldBorderWarningReachPacket(warningBlocks));

        float intervalFactor = (float) Math.pow(intensity, 0.85);
        int targetInterval = (int) (MAX_INTERVAL_TICKS - (intervalFactor * (MAX_INTERVAL_TICKS - MIN_INTERVAL_TICKS)));
        heartbeatTicks++;

        if (heartbeatTicks >= targetInterval) {
            playHeartbeatSound(intensity);
            heartbeatTicks = 0;
        }
    }

    private void playHeartbeatSound(float intensity) {
        float volume = 0.4f + (intensity * 0.6f);
        float randomPitchOffset = (float) (ThreadLocalRandom.current().nextDouble(-0.03, 0.03));
        float pitch = 0.75f + (intensity * 0.30f) + randomPitchOffset;

        Sound heartbeat = Sound.sound(
                SoundEvent.ENTITY_WARDEN_HEARTBEAT,
                Sound.Source.MASTER,
                volume,
                pitch
        );

        playSound(heartbeat, getPosition());
    }

    private void resetHeartbeat() {
        heartbeatActive = false;
        heartbeatTicks = 0;
        sendPacket(new WorldBorderWarningReachPacket(0));
    }

    /**
     * Returns whether the heartbeat effect is currently active for this player.
     *
     * @return {@code true} if heartbeat is active, otherwise {@code false}.
     */
    public boolean isHeartbeatActive() {
        return heartbeatActive;
    }

    @Override
    public EntityAttributesPacket getPropertiesPacket() {
        return super.getPropertiesPacket();
    }
}

