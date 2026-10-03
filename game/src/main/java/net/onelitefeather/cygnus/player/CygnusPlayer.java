package net.onelitefeather.cygnus.player;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.network.packet.server.play.EntityAttributesPacket;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.server.sound.SoundEvent;
import net.onelitefeather.cygnus.common.player.InstanceSwitchChunkPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static net.onelitefeather.cygnus.common.util.Helper.getRandomPitchValue;

@SuppressWarnings("java:S3252")
public final class CygnusPlayer extends InstanceSwitchChunkPlayer {

    static final AttributeModifier SPEED_MODIFIER_SPRINTING =
            new AttributeModifier(Key.key("cygnus","sprinting"), 0.25, AttributeOperation.ADD_MULTIPLIED_TOTAL);

    static final AttributeModifier DISABLED_SPRINT_MODIFIER =
            new AttributeModifier(Key.key("cygnus", "sprinting"), 0.0, AttributeOperation.ADD_MULTIPLIED_TOTAL);

    private static final float HEALTH_THRESHOLD = 6.0f; // 3 hearts
    private static final int MAX_INTERVAL_TICKS = 36;   // Every 1.8s (slow, subtle pulse at start)
    private static final int MIN_INTERVAL_TICKS = 12;   // Every 0.6s (fast & tense without sound overlapping)

    private static final int CALM_AMBIENT_INTERVAL_TICKS = 300;      // Every 15s, the old fixed rhythm
    private static final int TERRIFIED_AMBIENT_INTERVAL_TICKS = 100; // Every 5s
    private static final double AMBIENT_JITTER = 0.2D;               // Up to 20 percent shorter or longer
    private static final double EERIE_DREAD_THRESHOLD = 0.6D;        // The creek's default hunt threshold
    private static final SoundEvent[] EERIE_SOUNDS = {
            SoundEvent.AMBIENT_SOUL_SAND_VALLEY_MOOD,
            SoundEvent.AMBIENT_BASALT_DELTAS_MOOD,
            SoundEvent.ENTITY_WARDEN_LISTENING
    };

    private final @Nullable UUID resourcePackId;

    private boolean blockedSprinting;
    private int heartbeatTicks;
    private boolean heartbeatActive;
    private int ambientTicks;
    private double ambientJitter;

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
        this.ambientTicks = 0;
        this.ambientJitter = rollAmbientJitter();
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
     * Updates the heartbeat sound on player tick.
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
     * Plays the ambient sounds on player tick.
     *
     * <p>The more scared the player is, the shorter the gap between two sounds: from
     * {@value #CALM_AMBIENT_INTERVAL_TICKS} ticks when calm down to
     * {@value #TERRIFIED_AMBIENT_INTERVAL_TICKS} ticks when terrified, each gap randomly up to
     * 20 percent shorter or longer. The dread is read every tick, so a sudden scare shortens the
     * running gap right away. From {@value #EERIE_DREAD_THRESHOLD} on, eerier sounds join the
     * cave.</p>
     *
     * @param dread how scared the player is, between {@code 0} and {@code 1}
     */
    public void tickAmbient(double dread) {
        double fear = Math.clamp(dread, 0.0D, 1.0D);
        double interval = CALM_AMBIENT_INTERVAL_TICKS
                - fear * (CALM_AMBIENT_INTERVAL_TICKS - TERRIFIED_AMBIENT_INTERVAL_TICKS);
        ambientTicks++;

        if (ambientTicks >= interval * ambientJitter) {
            playAmbientSound(fear);
            ambientTicks = 0;
            ambientJitter = rollAmbientJitter();
        }
    }

    private void playAmbientSound(double fear) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        SoundEvent event = fear >= EERIE_DREAD_THRESHOLD && random.nextBoolean()
                ? EERIE_SOUNDS[random.nextInt(EERIE_SOUNDS.length)]
                : SoundEvent.AMBIENT_CAVE;
        playSound(Sound.sound(event, Sound.Source.MASTER, 1F, getRandomPitchValue()), getPosition());
    }

    private static double rollAmbientJitter() {
        return ThreadLocalRandom.current().nextDouble(1.0D - AMBIENT_JITTER, 1.0D + AMBIENT_JITTER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityAttributesPacket getPropertiesPacket() {
        return super.getPropertiesPacket();
    }
}

