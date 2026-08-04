package net.onelitefeather.cygnus.jumpscare;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket;
import net.minestom.server.network.packet.server.play.EntityHeadLookPacket;
import net.minestom.server.network.packet.server.play.ParticlePacket;

import net.minestom.server.particle.Particle;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.timer.TaskSchedule;
import net.onelitefeather.cygnus.entity.DeadPlayerMannequin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages dead player mannequins and turnaround scare for surviving players.
 *
 * @author theEvilReaper
 * @version 1.1.0
 * @since 2.7.0
 */
public final class JumpScareManager {

    private static final long COOLDOWN_MS = 60_000L; // 60 seconds
    private static final float TRIGGER_CHANCE = 0.35f; // 35% chance on rapid turnaround
    private static final float TURNAROUND_THRESHOLD_DEGREES = 120.0f;

    private final List<DeadPlayerMannequin> activeMannequins;
    private final Map<UUID, Long> jumpScareCooldowns;
    private final Map<UUID, Float> playerLastYaws;
    private final Random random;

    /**
     * Creates a new instance of the service.
     */
    public JumpScareManager() {
        this.activeMannequins = new CopyOnWriteArrayList<>();
        this.jumpScareCooldowns = new HashMap<>();
        this.playerLastYaws = new HashMap<>();
        this.random = new Random();
    }

    /**
     * Adds an {@link DeadPlayerMannequin} instance to the manager
     *
     * @param mannequin to add
     */
    public void register(DeadPlayerMannequin mannequin) {
        this.activeMannequins.add(mannequin);
    }

    /**
     * Removes an {@link DeadPlayerMannequin} instance from the manager
     *
     * @param mannequin to remove
     */
    public void unregister(DeadPlayerMannequin mannequin) {
        this.activeMannequins.remove(mannequin);
    }

    /**
     * Checks if the provided {@link Player} makes a turn around.
     *
     * @param player to check
     */
    public void checkTurnAround(Player player) {
        if (activeMannequins.isEmpty()) return;

        float currentYaw = player.getPosition().yaw();
        float previousYaw = playerLastYaws.getOrDefault(player.getUuid(), currentYaw);
        playerLastYaws.put(player.getUuid(), currentYaw);

        float yawDelta = Math.abs(currentYaw - previousYaw);
        if (yawDelta > 180.0f) {
            yawDelta = 360.0f - yawDelta;
        }

        if (yawDelta >= TURNAROUND_THRESHOLD_DEGREES) {
            triggerIfEligible(player);
        }
    }

    /**
     * Checks if a scare could be executed on a {@link Player}.
     *
     * @param player who is involved
     */
    private void triggerIfEligible(Player player) {
        long now = System.currentTimeMillis();
        long lastScare = jumpScareCooldowns.getOrDefault(player.getUuid(), 0L);

        if (now - lastScare < COOLDOWN_MS) {
            return;
        }

        if (random.nextFloat() > TRIGGER_CHANCE) {
            return;
        }

        execute(player);
    }

    /**
     * Force a scare to a provided {@link Player}.
     *
     * @param player who retrieves it
     * @return true when the execution was successfully otherwise false
     */
    public boolean force(Player player) {
        return execute(player);
    }

    /**
     * Executes the logic of the scare
     *
     * @param player who retrieves it
     * @return true when the execution was successfully otherwise false
     */
    private boolean execute(Player player) {
        if (activeMannequins.isEmpty()) return false;

        DeadPlayerMannequin sampleCorpse = activeMannequins.get(random.nextInt(activeMannequins.size()));
        if (sampleCorpse.getInstance() == null || sampleCorpse.isRemoved()) {
            activeMannequins.remove(sampleCorpse);
            return false;
        }

        jumpScareCooldowns.put(player.getUuid(), System.currentTimeMillis());

        Pos jumpscarePos = getJumpscarePos(player);

        DeadPlayerMannequin phantom = DeadPlayerMannequin.standing(
                sampleCorpse.getOriginalPlayerUuid(),
                sampleCorpse.getSkin(),
                jumpscarePos
        );
        int phantomEntityId = phantom.getEntityId();

        sampleCorpse.updateViewableRule(viewer -> !viewer.equals(player));

        player.sendPacket(phantom.toSpawnPacket());
        player.sendPacket(phantom.getMetadataPacket());
        player.sendPacket(new EntityHeadLookPacket(phantomEntityId, jumpscarePos.yaw()));

        // Initial horror sound effect & Darkness
        player.playSound(Sound.sound(
                Key.key("entity.elder_guardian.curse"),
                Sound.Source.HOSTILE,
                1.0f,
                0.6f
        ));
        player.addEffect(new Potion(PotionEffect.DARKNESS, (byte) 1, 40));

        // Initial smoke pulse at spawn
        player.sendPacket(new ParticlePacket(
                Particle.SMOKE, true, false,
                jumpscarePos.x(), jumpscarePos.y() + 1.0, jumpscarePos.z(),
                0.3f, 0.5f, 0.3f, 0.05f, 25
        ));

        // Schedule smooth despawn transition after 2.5 seconds (50 ticks)
        MinecraftServer.getSchedulerManager().buildTask(() -> {
            if (player.isOnline()) {
                // Smoke & Poof particle burst covering full body height
                player.sendPacket(new ParticlePacket(
                        Particle.POOF, true, false,
                        jumpscarePos.x(), jumpscarePos.y() + 1.0, jumpscarePos.z(),
                        0.4f, 0.8f, 0.4f, 0.05f, 35
                ));
                player.sendPacket(new ParticlePacket(
                        Particle.LARGE_SMOKE, true, false,
                        jumpscarePos.x(), jumpscarePos.y() + 1.0, jumpscarePos.z(),
                        0.3f, 0.6f, 0.3f, 0.02f, 20
                ));
                // Ghostly dissolve sound
                player.playSound(Sound.sound(
                        Key.key("block.fire.extinguish"),
                        Sound.Source.HOSTILE,
                        0.7f,
                        0.5f
                ));
                player.sendPacket(new DestroyEntitiesPacket(phantomEntityId));
            }
            restoreCorpseVisibility(sampleCorpse);
        }).delay(TaskSchedule.tick(50)).schedule();

        return true;
    }

    /**
     * Extracts the position for the jump scare from a {@link Player} target.
     *
     * @param player which is the target
     * @return extracted position
     */
    private static Pos getJumpscarePos(Player player) {
        Pos playerPos = player.getPosition();
        // Calculate strictly 2D horizontal forward vector on XZ plane at exact foot Y level
        float yawRad = (float) Math.toRadians(playerPos.yaw());
        double dirX = -Math.sin(yawRad);
        double dirZ = Math.cos(yawRad);
        double distance = 2.2; // 2.2 blocks in front

        double phantomX = playerPos.x() + dirX * distance;
        double phantomZ = playerPos.z() + dirZ * distance;
        double eyeHeight = player.getEyeHeight();

        Pos eyeToEyeView = new Pos(phantomX, playerPos.y() + eyeHeight, phantomZ)
                .withLookAt(playerPos.add(0, eyeHeight, 0));

        Pos jumpscarePos = new Pos(
                phantomX,
                playerPos.y(),
                phantomZ,
                eyeToEyeView.yaw(),
                eyeToEyeView.pitch()
        );
        return jumpscarePos;
    }

    /**
     * Updates corpse visibility of a given {@link DeadPlayerMannequin}.
     *
     * @param corpse to update
     */
    private void restoreCorpseVisibility(DeadPlayerMannequin corpse) {
        if (corpse.getInstance() != null && !corpse.isRemoved()) {
            corpse.updateViewableRule(null);
        }
    }

    /**
     * +
     * Removes all existing mannequin from the {@link Instance}.
     */
    public void cleanUp() {
        for (DeadPlayerMannequin mannequin : activeMannequins) {
            if (mannequin.getInstance() != null && !mannequin.isRemoved()) {
                mannequin.remove();
            }
        }
        activeMannequins.clear();
        jumpScareCooldowns.clear();
        playerLastYaws.clear();
    }

    /**
     * Returns an unmodifiable view of each active mannequin.
     *
     * @return the given list
     */
    @Contract(value = "-> new", pure = true)
    public @NotNull List<DeadPlayerMannequin> getActiveMannequins() {
        return Collections.unmodifiableList(activeMannequins);
    }
}
