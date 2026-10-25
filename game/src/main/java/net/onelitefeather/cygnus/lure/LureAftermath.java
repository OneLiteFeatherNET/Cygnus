package net.onelitefeather.cygnus.lure;

import net.minestom.server.entity.Player;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.onelitefeather.cygnus.common.config.LureConfig;
import net.onelitefeather.cygnus.creek.CreekRespite;
import net.onelitefeather.cygnus.creek.consequence.SlenderReveal;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * The consequence of a lure in a round: a jump in fear and a breather from the creek when the figure
 * strikes, a random follow-up once the phantom has dissolved.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public final class LureAftermath implements LureConsequence {

    private final LureConfig config;
    private final BiConsumer<UUID, Double> fear;
    private final CreekRespite respite;
    private final Supplier<@Nullable Player> slender;
    private final SlenderReveal reveal;
    private final Supplier<LureOutcome> outcomes;

    /**
     * Sets up the consequence.
     *
     * @param config   the settings
     * @param fear     raises a survivor's fear by the given gain
     * @param respite  the breather from the creek, shared with it
     * @param slender  supplies the slender, or {@code null} while there is none
     * @param reveal   shows a survivor to the slender for a betrayal
     * @param outcomes picks the follow-up of a scare
     */
    public LureAftermath(LureConfig config, BiConsumer<UUID, Double> fear, CreekRespite respite,
                         Supplier<@Nullable Player> slender, SlenderReveal reveal, Supplier<LureOutcome> outcomes) {
        this.config = config;
        this.fear = fear;
        this.respite = respite;
        this.slender = slender;
        this.reveal = reveal;
        this.outcomes = outcomes;
    }

    @Override
    public void struck(Player survivor, long now) {
        this.fear.accept(survivor.getUuid(), this.config.fearGain());
        this.respite.spare(survivor.getUuid(), now + this.config.respiteSeconds() * 1000L);
    }

    @Override
    public void followUp(Player survivor) {
        int ticks = this.config.effectSeconds() * 20;
        switch (this.outcomes.get()) {
            case SLOWNESS -> survivor.addEffect(new Potion(PotionEffect.SLOWNESS, (byte) 1, ticks));
            case BLINDNESS -> survivor.addEffect(new Potion(PotionEffect.BLINDNESS, (byte) 0, ticks));
            case BETRAYAL -> {
                Player hunter = this.slender.get();
                if (hunter != null) this.reveal.reveal(survivor, hunter);
            }
            case NOTHING -> {
                // The scare was all.
            }
        }
    }

    /**
     * Forgets every breather and ends every reveal, so the next round starts clean.
     */
    @Override
    public void cleanUp() {
        this.respite.clear();
        this.reveal.cleanUp();
    }
}
