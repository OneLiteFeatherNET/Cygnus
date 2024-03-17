package net.onelitefeather.cygnus.stamina;

import de.icevizion.aves.util.Components;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.attribute.Attribute;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.potion.TimedPotion;
import net.minestom.server.sound.SoundEvent;
import net.onelitefeather.cygnus.utils.Tags;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.ChronoUnit;
import java.util.function.BiFunction;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since
 **/
@SuppressWarnings({"java:S3252"})
public non-sealed class SlenderBar extends StaminaBar {

    private static final TimedPotion NIGHT_VISION = new TimedPotion(new Potion(PotionEffect.NIGHT_VISION, (byte) 1, -1), 0);
    private static final TimedPotion BLINDNESS = new TimedPotion(new Potion(PotionEffect.BLINDNESS, (byte) 1, -1), 0);
    private static final Sound TELEPORT = Sound.sound(SoundEvent.ENTITY_ENDERMAN_TELEPORT, Sound.Source.MASTER, 1F, 0.2F);
    private static final Sound SPAWN = Sound.sound(SoundEvent.ENTITY_WITHER_SPAWN, Sound.Source.MASTER, 0.4F, 0F);
    private static final Sound LEVEL = Sound.sound(SoundEvent.ENTITY_PLAYER_LEVELUP, Sound.Source.MASTER, 1F, 1F);

    // Constants
    private static final int MAX_TIME = 16;
    private static final float TIME_STEP = 0.5f;
    private final String tileChar;
    private final int time;
    private BiFunction<Player, Status, Void> accept;
    private double currentTime;

    SlenderBar(@NotNull Player player) {
        super(player, ChronoUnit.MILLIS, 500);
        this.tileChar = "▋";
        this.time = MAX_TIME;
        this.currentTime = time;
    }

    public void setAccept(@NotNull BiFunction<Player, Status, Void> accept) {
        this.accept = accept;
    }

    @Override
    protected void onStart() {
        this.status = Status.READY;
        this.player.addEffect(NIGHT_VISION.getPotion());
    }

    @Override
    public void consume() {
        switch (status) {
            case DRAINING -> {
                if (currentTime >= 0) {
                    currentTime -= TIME_STEP;
                    var nearbyEntities = player.getInstance().getNearbyEntities(player.getPosition(), 3);

                    if (nearbyEntities.isEmpty()) return;

                    for (Entity nearbyEntity : nearbyEntities) {
                        if (nearbyEntity instanceof Player target && !player.getUuid().equals(target.getUuid()) && (target.getHealth() > 0)) {
                            target.setHealth(target.getHealth() - TIME_STEP);
                        }
                    }
                } else {
                    status = Status.REGENERATING;
                    this.accept.apply(player, status);
                    player.setTag(Tags.HIDDEN, (byte) 0);
                    player.removeEffect(BLINDNESS.getPotion().effect());
                    player.addEffect(NIGHT_VISION.getPotion());
                }
                this.sendProgressBar();
            }
            case REGENERATING -> {
                if (currentTime < time + TIME_STEP) {
                    currentTime += TIME_STEP;
                } else {
                    status = Status.READY;
                    player.playSound(LEVEL, player.getPosition());
                }
                player.sendActionBar(Components.getProgressBar((int) currentTime, 17, 17, tileChar, NamedTextColor.GOLD, NamedTextColor.GRAY));
            }
            default -> {

            }
        }
    }

    public boolean changeStatus() {
        if (status == Status.REGENERATING && this.time <= 10) return false;
        switch (status) {
            case READY -> {
                status = Status.DRAINING;
                player.setTag(Tags.HIDDEN, (byte)1);
                player.removeEffect(NIGHT_VISION.getPotion().effect());
                player.addEffect(BLINDNESS.getPotion());
                player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.1f);
                this.accept.apply(player, Status.DRAINING);
            }
            case REGENERATING -> {
                status = Status.DRAINING;
                player.setTag(Tags.HIDDEN, (byte)1);
                playSoundToTarget(false);
                this.accept.apply(player, Status.DRAINING);
            }
            case DRAINING -> {
                status = Status.REGENERATING;
                player.setTag(Tags.HIDDEN, (byte)0);
                playSoundToTarget(true);
                player.removeEffect(BLINDNESS.getPotion().effect());
                player.addEffect(NIGHT_VISION.getPotion());
                this.accept.apply(player, Status.REGENERATING);
            }
        }
        return true;
    }

    private void playSoundToTarget(boolean spawn) {
        var nearbyEntities = player.getInstance().getNearbyEntities(player.getPosition(), 2);

        if (nearbyEntities.isEmpty()) return;

        for (Entity nearbyEntity : nearbyEntities) {
            if (nearbyEntity instanceof Player target && !target.getUuid().equals(player.getUuid())) {
                target.playSound(spawn ? SPAWN : TELEPORT, target.getPosition());
            }
        }
    }

    private void sendProgressBar() {
        player.sendActionBar(Components.getProgressBar((int) currentTime, 17, 17, tileChar, NamedTextColor.GREEN, NamedTextColor.GRAY));
    }
}
