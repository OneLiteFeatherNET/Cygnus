package net.onelitefeather.cygnus.stamina;

import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.instance.Instance;
import net.minestom.server.sound.SoundEvent;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.util.Helper;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.ChronoUnit;
import java.util.function.BiFunction;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
@SuppressWarnings("java:S3252")
public non-sealed class SlenderBar extends StaminaBar implements SlenderBarHelper {

    private static final Sound LEVEL = Sound.sound(SoundEvent.ENTITY_PLAYER_LEVELUP, Sound.Source.MASTER, 1F, 1F);

    // Constants
    private static final int MAX_TIME = 16;
    private static final float TIME_STEP = 0.5f;
    private final String tileChar;
    private final int time;
    private BiFunction<Player, State, Void> accept;
    private double currentTime;
    private StaminaColors colorState;

    SlenderBar(@NotNull CygnusPlayer player) {
        super(player, ChronoUnit.MILLIS, 500);
        this.tileChar = "▋";
        this.time = MAX_TIME;
        this.currentTime = time;
        this.colorState = StaminaColors.DRAINING;
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
            this.colorState.sendProgressBar(player, tileChar, (int) currentTime);
            return;
        }
        state = State.REGENERATING;
        colorState = StaminaColors.REGENERATING;
        player.setTag(Tags.HIDDEN, Helper.ONE_ID);
        this.accept.apply(player, state);
        this.applyNightVision(player);
        player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.1f);
        player.sendSpringPackets();
        player.setBlockedSprinting(false);
        this.colorState.sendProgressBar(player, tileChar, (int) currentTime);
    }

    private void handleRegeneration() {
        this.colorState.sendProgressBar(player, tileChar, (int) currentTime);
        if (currentTime <= time + TIME_STEP) {
            currentTime += TIME_STEP;
        } else {
            state = State.READY;
            colorState = StaminaColors.DRAINING;
            player.playSound(LEVEL, player.getPosition());

        }
    }

    public boolean changeStatus() {
        if (state == State.REGENERATING && this.time <= 10) return false;
        switch (state) {
            case READY -> {
                state = State.DRAINING;
                colorState = StaminaColors.DRAINING;
                player.setTag(Tags.HIDDEN, Helper.ONE_ID);
                this.applyBlindness(player);
                player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.0669);
                player.sendSpringPackets();
                player.setSprinting(false);
                player.setBlockedSprinting(true);
                this.accept.apply(player, State.DRAINING);
            }
            case REGENERATING -> {
                state = State.DRAINING;
                colorState = StaminaColors.DRAINING;
                player.setTag(Tags.HIDDEN, Helper.ONE_ID);
                this.playSpawnSound(player.getInstance(), Pos.fromPoint(player.getPosition()), player.getUuid());
                this.applyBlindness(player);
                player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.0669);
                player.sendSpringPackets();
                player.setSprinting(false);
                player.setBlockedSprinting(true);
                this.accept.apply(player, State.DRAINING);
            }
            case DRAINING -> {
                state = State.REGENERATING;
                colorState = StaminaColors.REGENERATING;
                player.setTag(Tags.HIDDEN, Helper.ZERO_ID);
                this.playTeleportSound(player.getInstance(), Pos.fromPoint(player.getPosition()), player.getUuid());
                this.applyNightVision(player);
                player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.1f);
                player.sendSpringPackets();
                player.setBlockedSprinting(false);
                this.accept.apply(player, State.REGENERATING);
            }
        }
        return true;
    }
}
