package net.onelitefeather.cygnus.jumpscare;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
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
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
    private static final double PHANTOM_SPAWN_DISTANCE = 2.2; // blocks in front of the victim
    private static final double MIN_PHANTOM_SPAWN_DISTANCE = 0.8; // never spawn closer than this
    private static final double COLLISION_CHECK_STEP = 0.2; // ray-march resolution against blocks

    private final List<DeadPlayerMannequin> activeMannequins;
    private final Map<UUID, Long> jumpScareCooldowns;
    private final Map<UUID, Float> playerLastYaws;
    private final Map<DeadPlayerMannequin, Set<UUID>> corpseHiddenFromViewers;
    private final Random random;

    /**
     * Creates a new instance of the service.
     */
    public JumpScareManager() {
        this.activeMannequins = new CopyOnWriteArrayList<>();
        this.jumpScareCooldowns = new ConcurrentHashMap<>();
        this.playerLastYaws = new ConcurrentHashMap<>();
        this.corpseHiddenFromViewers = new ConcurrentHashMap<>();
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

        activeMannequins.removeIf(corpse -> corpse.getInstance() == null || corpse.isRemoved());
        if (activeMannequins.isEmpty()) return false;

        // No free spot to place the phantom in (e.g. the victim is pressed flush against a wall)
        Pos jumpscarePos = getJumpscarePos(player);
        if (jumpscarePos == null) return false;

        DeadPlayerMannequin sampleCorpse = activeMannequins.get(random.nextInt(activeMannequins.size()));

        jumpScareCooldowns.put(player.getUuid(), System.currentTimeMillis());

        DeadPlayerMannequin phantom = DeadPlayerMannequin.standing(
                sampleCorpse.getOriginalPlayerUuid(),
                sampleCorpse.getSkin(),
                jumpscarePos
        );
        int phantomEntityId = phantom.getEntityId();

        hideCorpseFromViewer(sampleCorpse, player);

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
            restoreCorpseVisibility(sampleCorpse, player);
        }).delay(TaskSchedule.tick(50)).schedule();

        return true;
    }

    /**
     * Extracts the position for the jump scare from a {@link Player} target.
     *
     * @param player which is the target
     * @return the extracted position, or null if there is no free spot in front of the player
     *         to place the phantom (e.g. the player stands flush against a wall)
     */
    private static @Nullable Pos getJumpscarePos(Player player) {
        Pos playerPos = player.getPosition();
        // Calculate strictly 2D horizontal forward vector on XZ plane at exact foot Y level
        float yawRad = (float) Math.toRadians(playerPos.yaw());
        double dirX = -Math.sin(yawRad);
        double dirZ = Math.cos(yawRad);
        double distance = clampToFreeDistance(player.getInstance(), playerPos, dirX, dirZ);
        if (distance < MIN_PHANTOM_SPAWN_DISTANCE) return null;

        double phantomX = playerPos.x() + dirX * distance;
        double phantomZ = playerPos.z() + dirZ * distance;
        double eyeHeight = player.getEyeHeight();

        Pos eyeToEyeView = new Pos(phantomX, playerPos.y() + eyeHeight, phantomZ)
                .withLookAt(playerPos.add(0, eyeHeight, 0));

        return new Pos(
                phantomX,
                playerPos.y(),
                phantomZ,
                eyeToEyeView.yaw(),
                eyeToEyeView.pitch()
        );
    }

    /**
     * Walks from the player toward the candidate phantom spot in small steps and stops at the
     * last free position before solid geometry, so the phantom never spawns clipped into a wall.
     *
     * @param instance  the instance to check block collisions against
     * @param playerPos the player's position, used as the ray origin
     * @param dirX      the horizontal forward direction on the X axis
     * @param dirZ      the horizontal forward direction on the Z axis
     * @return the largest free distance up to {@link #PHANTOM_SPAWN_DISTANCE}; may be smaller
     *         than {@link #MIN_PHANTOM_SPAWN_DISTANCE} if solid geometry starts right away
     */
    private static double clampToFreeDistance(Instance instance, Pos playerPos, double dirX, double dirZ) {
        double freeDistance = PHANTOM_SPAWN_DISTANCE;
        for (double distance = COLLISION_CHECK_STEP; distance <= PHANTOM_SPAWN_DISTANCE; distance += COLLISION_CHECK_STEP) {
            double sampleX = playerPos.x() + dirX * distance;
            double sampleZ = playerPos.z() + dirZ * distance;
            Block feetBlock = instance.getBlock(new Pos(sampleX, playerPos.y(), sampleZ));
            Block headBlock = instance.getBlock(new Pos(sampleX, playerPos.y() + 1.5, sampleZ));
            if (feetBlock.solid() || headBlock.solid()) {
                freeDistance = distance - COLLISION_CHECK_STEP;
                break;
            }
        }
        return freeDistance;
    }

    /**
     * Hides a corpse from a single victim without affecting any other viewer's visibility rule,
     * even if another jumpscare is concurrently hiding the same corpse from a different victim.
     *
     * @param corpse the corpse to hide
     * @param victim the player it should be hidden from
     */
    private void hideCorpseFromViewer(DeadPlayerMannequin corpse, Player victim) {
        Set<UUID> hiddenFrom = corpseHiddenFromViewers.computeIfAbsent(corpse, _ -> ConcurrentHashMap.newKeySet());
        hiddenFrom.add(victim.getUuid());
        corpse.updateViewableRule(viewer -> !hiddenFrom.contains(viewer.getUuid()));
    }

    /**
     * Restores corpse visibility for a single victim, leaving the corpse hidden for any other
     * victim whose jumpscare against the same corpse is still active.
     *
     * @param corpse the corpse to update
     * @param victim the player it should become visible to again
     */
    private void restoreCorpseVisibility(DeadPlayerMannequin corpse, Player victim) {
        Set<UUID> hiddenFrom = corpseHiddenFromViewers.get(corpse);
        if (hiddenFrom == null) return;
        hiddenFrom.remove(victim.getUuid());

        if (corpse.getInstance() == null || corpse.isRemoved()) {
            corpseHiddenFromViewers.remove(corpse);
            return;
        }

        if (hiddenFrom.isEmpty()) {
            corpseHiddenFromViewers.remove(corpse);
            corpse.updateViewableRule(null);
        } else {
            corpse.updateViewableRule(viewer -> !hiddenFrom.contains(viewer.getUuid()));
        }
    }

    /**
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
        corpseHiddenFromViewers.clear();
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
