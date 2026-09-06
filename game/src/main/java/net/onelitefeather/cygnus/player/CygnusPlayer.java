package net.onelitefeather.cygnus.player;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.network.packet.server.play.EntityAttributesPacket;
import net.minestom.server.network.packet.server.play.InitializeWorldBorderPacket;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.server.sound.SoundEvent;
import net.onelitefeather.cygnus.common.player.InstanceSwitchChunkPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings("java:S3252")
public final class CygnusPlayer extends InstanceSwitchChunkPlayer {

    static final int PORTAL_TELEPORT_BOUNDARY = 29_999_984;

    /**
     * Radius (in blocks) of the virtual, per-player world border used to fake the heartbeat
     * vignette. It is centered on the player every tick, so the real client-side distance to
     * its edge is always exactly this value, independent of the instance's actual world border.
     */
    static final double FAKE_BORDER_RADIUS = 50.0;
    static final double FAKE_BORDER_DIAMETER = FAKE_BORDER_RADIUS * 2.0;

    static final AttributeModifier SPEED_MODIFIER_SPRINTING =
            new AttributeModifier(Key.key("cygnus","sprinting"), 0.25, AttributeOperation.ADD_MULTIPLIED_TOTAL);

    static final AttributeModifier DISABLED_SPRINT_MODIFIER =
            new AttributeModifier(Key.key("cygnus", "sprinting"), 0.0, AttributeOperation.ADD_MULTIPLIED_TOTAL);

    private static final float HEALTH_THRESHOLD = 6.0f; // 3 hearts
    private static final int MAX_INTERVAL_TICKS = 36;   // Every 1.8s (slow, subtle pulse at start)
    private static final int MIN_INTERVAL_TICKS = 12;   // Every 0.6s (fast & tense without sound overlapping)

    private final @Nullable UUID resourcePackId;

    private boolean blockedSprinting;
    private int heartbeatTicks;
    private boolean heartbeatActive;

    private int pageFounds;
    private int kills;
    private boolean death;

    /**
     * Creates a new player.
     *
     * @param playerConnection the connection the player is created for
     * @param gameProfile      the profile the player logged in with
     * @param resourcePackId   the id of the ResourcePack this service pushes, or {@code null} when
     *                         the ResourcePack feature is disabled
     */
    public CygnusPlayer(PlayerConnection playerConnection, GameProfile gameProfile, @Nullable UUID resourcePackId) {
        super(playerConnection, gameProfile);
        this.resourcePackId = resourcePackId;
        this.blockedSprinting = false;
        this.heartbeatTicks = 0;
        this.heartbeatActive = false;
        this.pageFounds = 0;
        this.kills = 0;
        this.death = false;
    }

    /**
     * Takes this service's ResourcePack off the client, then disconnects the player.
     *
     * <p>A client drops a pushed pack only when told to. On a bare connection that happens
     * implicitly - leaving the server ends the connection the pack hangs on - but behind a proxy the
     * connection survives: a kick makes the proxy move the player to another backend, and the pack
     * stays applied in the network lobby. So the pop has to be sent, and it has to be sent from
     * here: by the time {@code PlayerDisconnectEvent} fires, the disconnect packet is already queued
     * ahead of anything a listener could still send, and the client acts on the first of the two it
     * reads.</p>
     *
     * <p>The pop cannot be sent on the other exit either - a proxy switching backends closes this
     * connection without warning, leaving no moment to send anything. Only the lobby can clear the
     * pack for a player who leaves that way.</p>
     *
     * @param component the kick message
     */
    @Override
    public void kick(Component component) {
        if (this.resourcePackId != null) {
            removeResourcePacks(this.resourcePackId);
        }
        super.kick(component);
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
            this.sendSprintPackets();
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
        this.sendSprintPackets();
    }

    /**
     * Sends the packets to the player to update the sprinting state.
     */
    public void sendSprintPackets() {
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
     * Increments the number of pages this player has found in the current round.
     */
    public void incrementPageFound() {
        this.pageFounds++;
    }

    /**
     * Returns how many pages this player has found in the current round.
     *
     * @return the page count
     */
    public int getPageFounds() {
        return pageFounds;
    }

    /**
     * Increments the number of survivors this player has killed in the current round.
     */
    public void incrementKills() {
        this.kills++;
    }

    /**
     * Returns how many survivors this player has killed in the current round.
     *
     * @return the kill count
     */
    public int getKills() {
        return kills;
    }

    /**
     * Marks whether this player died during the current round.
     *
     * @param death {@code true} if the player died this round
     */
    public void setDeath(boolean death) {
        this.death = death;
    }

    /**
     * Checks if the player died during the current round.
     *
     * @return {@code true} if the player died this round, otherwise {@code false}.
     */
    public boolean hasDied() {
        return death;
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

        // Non-linear visual curve makes the red border vignette stronger earlier and very intense at low HP
        float visualIntensity = (float) Math.pow(intensity, 0.6);
        float clampedIntensity = Math.min(visualIntensity, 0.995f);
        int warningBlocks = (int) (FAKE_BORDER_RADIUS / (1.0f - clampedIntensity));

        var position = getPosition();
        sendPacket(new InitializeWorldBorderPacket(
                position.x(), position.z(),
                FAKE_BORDER_DIAMETER, FAKE_BORDER_DIAMETER, 0L,
                PORTAL_TELEPORT_BOUNDARY, 0, warningBlocks
        ));

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
        // Restores the real (instance-wide) world border after the per-tick fake one used for
        // the vignette, so the client stops seeing the small virtual border we sent it.
        sendPacket(getInstance().createInitializeWorldBorderPacket());
    }

    /**
     * Returns whether the heartbeat effect is currently active for this player.
     *
     * @return {@code true} if heartbeat is active, otherwise {@code false}.
     */
    public boolean isHeartbeatActive() {
        return heartbeatActive;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityAttributesPacket getPropertiesPacket() {
        return super.getPropertiesPacket();
    }
}

