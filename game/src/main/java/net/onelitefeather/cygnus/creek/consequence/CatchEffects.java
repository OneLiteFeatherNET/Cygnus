package net.onelitefeather.cygnus.creek.consequence;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.entity.Player;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.onelitefeather.cygnus.stamina.FoodBar;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The normal punishment for being caught: a jump scare, empty stamina and slowness.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class CatchEffects implements CatchConsequence {

    /** Sound of the fallback scare, used when no corpse exists yet. */
    static final Key SCARE_SOUND = Key.key("entity.creaking.activate");

    /** Duration of the fallback scare's darkness, in ticks. */
    static final int SCARE_DARKNESS_TICKS = 40;

    private final Predicate<Player> jumpScare;
    private final Function<Player, @Nullable FoodBar> foodBars;
    private final int slownessTicks;
    private final Set<Player> slowed = ConcurrentHashMap.newKeySet();

    /**
     * Creates the effects.
     *
     * @param jumpScare       plays the jump scare and returns {@code false} if it failed
     * @param foodBars        returns a survivor's stamina bar, or {@code null} if there is none
     * @param slownessSeconds how long a caught survivor is slowed
     */
    public CatchEffects(Predicate<Player> jumpScare, Function<Player, @Nullable FoodBar> foodBars, int slownessSeconds) {
        this.jumpScare = jumpScare;
        this.foodBars = foodBars;
        this.slownessTicks = slownessSeconds * 20;
    }

    @Override
    public void apply(Player survivor) {
        // The jump scare needs a corpse, so it does nothing before the first death.
        // The fallback makes sure the catch is still noticeable.
        if (!this.jumpScare.test(survivor)) {
            scareWithoutCorpse(survivor);
        }
        FoodBar bar = this.foodBars.apply(survivor);
        if (bar != null) {
            bar.drain();
        }
        if (this.slownessTicks > 0) {
            survivor.addEffect(new Potion(PotionEffect.SLOWNESS, 0, this.slownessTicks));
            this.slowed.add(survivor);
        }
    }

    @Override
    public void cleanUp() {
        for (Player player : List.copyOf(this.slowed)) {
            player.removeEffect(PotionEffect.SLOWNESS);
        }
        this.slowed.clear();
    }

    private static void scareWithoutCorpse(Player survivor) {
        survivor.playSound(Sound.sound(SCARE_SOUND, Sound.Source.HOSTILE, 1.0F, 0.6F));
        survivor.addEffect(new Potion(PotionEffect.DARKNESS, 0, SCARE_DARKNESS_TICKS));
    }
}
