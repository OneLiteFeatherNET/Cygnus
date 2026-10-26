package net.onelitefeather.cygnus.creek.state;

import net.onelitefeather.cygnus.common.config.CreekConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreekTuningTest {

    private static final CreekConfig CONFIG = CreekConfig.DEFAULT;
    private static final CreekTuning FULL = new CreekTuning(CONFIG, Calm.NONE);
    private static final CreekTuning CALMEST = new CreekTuning(CONFIG, new Calm(1.0D));

    @Test
    @DisplayName("The hunter hunts sooner, faster and again sooner, and creeps on while watched")
    void hunterWeights() {
        CreekTuning hunter = new CreekTuning(CONFIG, Calm.NONE, CreekMood.HUNTER);

        assertEquals(CONFIG.hunt().speed() * 1.2D, hunter.huntSpeed(), 1.0E-9);
        assertEquals(CONFIG.hunt().threshold() * 0.7D, hunter.huntThreshold(), 1.0E-9);
        assertEquals(Math.round(CONFIG.hunt().minStalkSeconds() * 1000L * 0.5D), hunter.huntMinStalkMillis());
        assertEquals(6_000L, hunter.huntCooldown(10_000L));
        assertEquals(CONFIG.hunt().speed() * 1.2D * 0.25D, hunter.watchedHuntSpeed(), 1.0E-9);
    }

    @Test
    @DisplayName("The shadow stalks longer, haunts again sooner and hunts later")
    void shadowWeights() {
        CreekTuning shadow = new CreekTuning(CONFIG, Calm.NONE, CreekMood.SHADOW);

        assertEquals(Math.round(CONFIG.stalk().minSeconds() * 1000L * 1.5D), shadow.stalkMillis(Contexts.rolling(0.0D)));
        assertEquals(28_000L, shadow.variantBreatherMillis(0.0D));
        assertEquals(CONFIG.hunt().threshold() * 1.3D, shadow.huntThreshold(), 1.0E-9);
    }

    @Test
    @DisplayName("The calm still applies on top of a mood")
    void calmStacksOnMood() {
        CreekTuning hunter = new CreekTuning(CONFIG, new Calm(1.0D), CreekMood.HUNTER);

        assertEquals(CONFIG.hunt().speed() * 1.2D * 0.8D, hunter.huntSpeed(), 1.0E-9);
        assertEquals(9_000L, hunter.huntCooldown(10_000L));
    }

    @Test
    @DisplayName("The normal creek does not creep while watched, and only the trickster vanishes more at dead ends")
    void normalAndTrickster() {
        assertEquals(0.0D, FULL.watchedHuntSpeed());
        assertEquals(0.5D, FULL.deadEndVanishChance());
        assertEquals(0.8D, new CreekTuning(CONFIG, Calm.NONE, CreekMood.TRICKSTER).deadEndVanishChance());
    }

    @Test
    @DisplayName("The hunt speed slows down with the calm")
    void huntSpeed() {
        assertEquals(CONFIG.hunt().speed(), FULL.huntSpeed(), 1.0E-9);
        assertEquals(CONFIG.hunt().speed() * 0.8D, CALMEST.huntSpeed(), 1.0E-9);
    }

    @Test
    @DisplayName("The hunt threshold and the shortest stalk before a hunt come from the config")
    void huntStart() {
        assertEquals(CONFIG.hunt().threshold(), CALMEST.huntThreshold(), 1.0E-9);
        assertEquals(CONFIG.hunt().minStalkSeconds() * 1000L, CALMEST.huntMinStalkMillis());
    }

    @Test
    @DisplayName("The breather after a hunt stretches with the calm")
    void huntCooldown() {
        assertEquals(10_000L, FULL.huntCooldown(10_000L));
        assertEquals(15_000L, CALMEST.huntCooldown(10_000L));
    }

    @Test
    @DisplayName("Waits and distances stretch with the calm")
    void stretches() {
        assertEquals(CONFIG.stalk().revealMillis(), FULL.revealMillis());
        assertEquals(Math.round(CONFIG.stalk().revealMillis() * 1.5D), CALMEST.revealMillis());
        assertEquals(Math.round(CONFIG.stuckMillis() * 1.5D), CALMEST.stuckMillis());
        assertEquals(30.0D, CALMEST.stretch(20.0D), 1.0E-9);
    }

    @Test
    @DisplayName("A stalk lasts between the shortest and the longest stalk of the config")
    void stalkMillis() {
        assertEquals(CONFIG.stalk().minSeconds() * 1000L, FULL.stalkMillis(Contexts.rolling(0.0D)));
    }

    @Test
    @DisplayName("The breather after a variant runs from the longest vanish for a calm survivor to the shortest for a scared one")
    void variantBreather() {
        assertEquals(40_000L, FULL.variantBreatherMillis(0.0D));
        assertEquals(20_000L, FULL.variantBreatherMillis(1.0D));
        assertEquals(60_000L, CALMEST.variantBreatherMillis(0.0D));
    }
}
