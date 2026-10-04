package net.onelitefeather.cygnus.player;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.resource.ResourcePackStatus;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.network.ConnectionState;
import net.minestom.server.network.packet.server.play.EntityAttributesPacket;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.TaskSchedule;
import net.onelitefeather.cygnus.common.player.InstanceSwitchChunkPlayer;
import net.onelitefeather.cygnus.telemetry.CygnusAttributes;
import net.onelitefeather.cygnus.telemetry.KickTracer;
import net.onelitefeather.cygnus.telemetry.TraceStep;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

import static net.onelitefeather.cygnus.common.util.Helper.getRandomPitchValue;

@SuppressWarnings("java:S3252")
public final class CygnusPlayer extends InstanceSwitchChunkPlayer {

    static final AttributeModifier SPEED_MODIFIER_SPRINTING =
            new AttributeModifier(Key.key("cygnus","sprinting"), 0.25, AttributeOperation.ADD_MULTIPLIED_TOTAL);

    static final AttributeModifier DISABLED_SPRINT_MODIFIER =
            new AttributeModifier(Key.key("cygnus", "sprinting"), 0.0, AttributeOperation.ADD_MULTIPLIED_TOTAL);

    private static final int POP_TIMEOUT_TICKS = 10;           // Half a second for the client to drop the pack

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
    private final KickTracer kickTracer;

    private boolean leaving;
    // Atomic: the ack arrives on the network thread, the timeout on the tick thread, and the
    // disconnect on whichever closed the connection. Exactly one of them may take the kick.
    private final AtomicReference<@Nullable Component> pendingKick = new AtomicReference<>();
    private final AtomicReference<@Nullable TraceStep> kickStep = new AtomicReference<>();
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
        this(playerConnection, gameProfile, resourcePackId, KickTracer.NONE);
    }

    /**
     * Creates a new player whose kicks are traced.
     *
     * @param playerConnection the connection the player is created for
     * @param gameProfile      the profile the player logged in with
     * @param resourcePackId   the id of the ResourcePack this service pushes, or {@code null} when
     *                         the ResourcePack feature is disabled
     * @param kickTracer       creates the span of a kick
     * @since 2.15.0
     */
    public CygnusPlayer(PlayerConnection playerConnection, GameProfile gameProfile, @Nullable UUID resourcePackId,
                        KickTracer kickTracer) {
        super(playerConnection, gameProfile);
        this.resourcePackId = resourcePackId;
        this.kickTracer = kickTracer;
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
     * Takes this service's ResourcePack off the client, waits for the client to confirm that, then
     * disconnects the player.
     *
     * <p>A client drops a pushed pack only when told to. On a bare connection that happens
     * implicitly - leaving the server ends the connection the pack hangs on - but behind a proxy the
     * connection survives: a kick makes the proxy move the player to another backend, and the pack
     * stays applied in the network lobby. So the pop has to be sent, and it has to be sent from
     * here: by the time {@code PlayerDisconnectEvent} fires, the disconnect packet is already queued
     * ahead of anything a listener could still send, and the client acts on the first of the two it
     * reads.</p>
     *
     * <p>Sending the pop right before the disconnect is not enough, though: nothing guarantees the
     * client has processed it by the time the proxy reroutes the player. So the disconnect is held
     * back until the client reports the pack as discarded (see
     * {@link #onResourcePackStatus(UUID, ResourcePackStatus)}), or {@value #POP_TIMEOUT_TICKS}
     * ticks have passed - a client that never answers must not stay on the server, and the timeout
     * stays far below the second a restart waits before it stops the service. While the player is
     * leaving, further kicks are ignored: the first one decides the message, and the disconnect
     * happens exactly once.</p>
     *
     * <p>Without a pack, or outside the play state (a kick during configuration has no pack on the
     * client yet and no play packets to wait on), the player is disconnected immediately.</p>
     *
     * <p>The pop cannot be sent on the other exit either - a proxy switching backends closes this
     * connection without warning, leaving no moment to send anything. Only the lobby can clear the
     * pack for a player who leaves that way.</p>
     *
     * @param component the kick message
     */
    @Override
    public void kick(Component component) {
        if (this.leaving) {
            return;
        }
        if (this.resourcePackId == null || !isOnline()
                || getPlayerConnection().getServerState() != ConnectionState.PLAY) {
            // Nothing to wait for, so the span only records that the kick happened and why.
            TraceStep immediate = this.kickTracer.begin(getUuid(), component);
            try {
                super.kick(component);
            } finally {
                this.kickTracer.complete(immediate, CygnusAttributes.KICK_BY_IMMEDIATE);
            }
            return;
        }
        this.leaving = true;
        this.kickStep.set(this.kickTracer.begin(getUuid(), component));
        this.pendingKick.set(component);
        removeResourcePacks(this.resourcePackId);
        scheduler().buildTask(() -> completeKick(CygnusAttributes.KICK_BY_TIMEOUT)).delay(TaskSchedule.tick(POP_TIMEOUT_TICKS)).schedule();
    }

    /**
     * Returns whether this player is being kicked and only waits for the client to drop the pack.
     *
     * @return {@code true} while the disconnect is pending, otherwise {@code false}
     * @since 2.14.1
     */
    public boolean isLeaving() {
        return leaving;
    }

    /**
     * Treats the client's report about the popped pack as the confirmation the kick waits for.
     *
     * <p>Overridden here rather than listened to on the event bus because the player already holds
     * the pending kick, and this is called after every listener of
     * {@code PlayerResourcePackStatusEvent} has run. Minestom itself kicks a required pack that
     * ends in a non-successful terminal status; for a pop on a pack that finished loading earlier
     * it has no pending entry and does nothing, and for one still in flight it calls
     * {@link #kick(Component)}, which is ignored while leaving.</p>
     *
     * @param id     the pack the status is about
     * @param status the reported status
     * @since 2.14.1
     */
    @Override
    public void onResourcePackStatus(UUID id, ResourcePackStatus status) {
        if (this.leaving && id.equals(this.resourcePackId) && !status.intermediate()) {
            completeKick(CygnusAttributes.KICK_BY_ACK);
        }
        super.onResourcePackStatus(id, status);
    }

    /**
     * Lets the held-back disconnect through. Whichever of the ack and the timeout comes first wins;
     * the other finds nothing pending and does nothing, which also keeps the kick span from being
     * ended twice.
     *
     * @param completedBy what ended the wait, for the kick span
     */
    private void completeKick(String completedBy) {
        Component component = this.pendingKick.getAndSet(null);
        if (component == null) {
            return;
        }
        TraceStep step = this.kickStep.getAndSet(null);
        try {
            if (isOnline()) {
                super.kick(component);
            }
        } finally {
            if (step != null) {
                this.kickTracer.complete(step, completedBy);
            }
        }
    }

    /**
     * Ends the kick span of a client that disconnected on its own during the pack-drop wait, when
     * neither the ack nor the timeout will find anything to do.
     *
     * @param permanent whether the player leaves the server for good
     */
    @Override
    public void remove(boolean permanent) {
        if (permanent && this.pendingKick.getAndSet(null) != null) {
            TraceStep step = this.kickStep.getAndSet(null);
            if (step != null) {
                this.kickTracer.complete(step, CygnusAttributes.KICK_BY_DISCONNECTED);
            }
        }
        super.remove(permanent);
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

