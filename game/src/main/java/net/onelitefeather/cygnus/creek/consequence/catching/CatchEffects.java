package net.onelitefeather.cygnus.creek.consequence.catching;

import net.minestom.server.entity.Player;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.onelitefeather.cygnus.creek.consequence.CreekScare;
import net.onelitefeather.cygnus.creek.consequence.TrackedEffects;
import net.onelitefeather.cygnus.stamina.FoodBar;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The usual price for being caught: a jump scare, an empty stamina bar and slowness.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class CatchEffects implements CatchConsequence {

    /** How long the stand-in scare's darkness lasts, in ticks. */
    static final int SCARE_DARKNESS_TICKS = 40;

    private final Predicate<Player> jumpScare;
    private final Function<Player, @Nullable FoodBar> foodBars;
    private final int slownessTicks;
    private final TrackedEffects effects;

    /**
     * Sets up the effects.
     *
     * @param jumpScare       plays the jump scare and returns {@code false} if it could not
     * @param foodBars        returns a survivor's stamina bar, or {@code null} if they have none
     * @param slownessSeconds how long a caught survivor is slowed
     */
    public CatchEffects(Predicate<Player> jumpScare, Function<Player, @Nullable FoodBar> foodBars, int slownessSeconds) {
        this.jumpScare = jumpScare;
        this.foodBars = foodBars;
        this.slownessTicks = slownessSeconds * 20;
        this.effects = new TrackedEffects();
    }

    @Override
    public void apply(Player survivor) {
        // The jump scare needs a corpse, so before the first death there is nothing to show.
        // The stand-in makes sure a catch never goes unnoticed.
        if (!this.jumpScare.test(survivor)) {
            scareWithoutCorpse(survivor);
        }
        FoodBar bar = this.foodBars.apply(survivor);
        if (bar != null) {
            bar.drain();
        }
        if (this.slownessTicks > 0) {
            this.effects.add(survivor, new Potion(PotionEffect.SLOWNESS, 0, this.slownessTicks));
        }
    }

    @Override
    public void cleanUp() {
        this.effects.removeAll();
    }

    private static void scareWithoutCorpse(Player survivor) {
        CreekScare.play(survivor);
        survivor.addEffect(new Potion(PotionEffect.DARKNESS, 0, SCARE_DARKNESS_TICKS));
    }
}
