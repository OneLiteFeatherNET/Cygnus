package net.onelitefeather.cygnus.stamina;

import de.icevizion.aves.util.Components;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.instance.Instance;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.potion.TimedPotion;
import net.minestom.server.sound.SoundEvent;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.ChronoUnit;
import java.util.function.BiFunction;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
@SuppressWarnings({"java:S3252"})
public non-sealed class SlenderBar extends StaminaBar implements SlenderBarHelper {

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
    private BiFunction<Player, State, Void> accept;
    private double currentTime;
    private StaminarColors colorState;

    SlenderBar(@NotNull CygnusPlayer player) {
        super(player, ChronoUnit.MILLIS, 500);
        this.tileChar = "▋";
        this.time = MAX_TIME;
        this.currentTime = time;
        this.colorState = StaminarColors.DRAINING;
    }

    public void setAccept(@NotNull BiFunction<Player, State, Void> accept) {
        this.accept = accept;
    }

    @Override
    protected void onStart() {
        this.state = State.READY;
        this.player.addEffect(NIGHT_VISION.potion());
    }

    @Override
    public void consume() {
        if (state == State.READY) return;
        if (state == State.DRAINING) {
            this.handleDraining();
            return;
        }
        this.handleRegeneration();
    }

    private void handleDraining() {
        if (currentTime >= 0) {
            currentTime -= TIME_STEP;
            Instance instance = player.getInstance();
            applyDamage(instance, player.getUuid(), Pos.fromPoint(player.getPosition()), 3, TIME_STEP);
            return;
        }
        state = State.REGENERATING;
        colorState = StaminarColors.REGENERATING;
        this.accept.apply(player, state);
        player.setTag(Tags.HIDDEN, (byte) 0);
        player.removeEffect(BLINDNESS.potion().effect());
        player.addEffect(NIGHT_VISION.potion());
        this.colorState.sendProgressBar(player, tileChar, (int) currentTime);
    }

    private void handleRegeneration() {
        if (currentTime < time + TIME_STEP) {
            currentTime += TIME_STEP;
        } else {
            state = State.READY;
            colorState = StaminarColors.DRAINING;
            player.playSound(LEVEL, player.getPosition());
        }
        this.colorState.sendProgressBar(player, tileChar, (int) currentTime);
        player.sendActionBar(Components.getProgressBar((int) currentTime, 17, 17, tileChar, NamedTextColor.GOLD, NamedTextColor.GRAY));
    }

    public boolean changeStatus() {
        if (state == State.REGENERATING && this.time <= 10) return false;
        switch (state) {
            case READY -> {
                state = State.DRAINING;
                colorState = StaminarColors.DRAINING;
                player.setTag(Tags.HIDDEN, (byte) 1);
                player.removeEffect(NIGHT_VISION.potion().effect());
                player.addEffect(BLINDNESS.potion());
                player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.1f);
                this.accept.apply(player, State.DRAINING);
            }
            case REGENERATING -> {
                state = State.DRAINING;
                colorState = StaminarColors.DRAINING;
                player.setTag(Tags.HIDDEN, (byte) 1);
                playSoundToTarget(false);
                this.accept.apply(player, State.DRAINING);
            }
            case DRAINING -> {
                state = State.REGENERATING;
                colorState = StaminarColors.REGENERATING;
                player.setTag(Tags.HIDDEN, (byte) 0);
                playSoundToTarget(true);
                player.removeEffect(BLINDNESS.potion().effect());
                player.addEffect(NIGHT_VISION.potion());
                this.accept.apply(player, State.REGENERATING);
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
}
