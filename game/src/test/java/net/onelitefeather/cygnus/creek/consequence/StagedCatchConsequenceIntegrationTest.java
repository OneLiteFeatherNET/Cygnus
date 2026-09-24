package net.onelitefeather.cygnus.creek.consequence;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.CreekConfig;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StagedCatchConsequenceIntegrationTest extends CygnusPlayerTestBase {

    private final List<Player> punished = new ArrayList<>();
    private final List<Player[]> revealed = new ArrayList<>();
    private int cleanUps;

    private final CatchConsequence effects = new CatchConsequence() {
        @Override
        public void apply(Player survivor) {
            punished.add(survivor);
        }

        @Override
        public void cleanUp() {
            cleanUps++;
        }
    };

    private final SlenderReveal reveal = new SlenderReveal() {
        @Override
        public void reveal(Player survivor, Player slender) {
            revealed.add(new Player[]{survivor, slender});
        }

        @Override
        public void cleanUp() {
            cleanUps++;
        }
    };

    private StagedCatchConsequence consequence(@Nullable Player slender, double draw) {
        return new StagedCatchConsequence(effects, reveal, () -> slender, CreekConfig.DEFAULT, always(draw));
    }

    @Test
    @DisplayName("The first catch only punishes")
    void firstCatchPunishes(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        Player slender = env.createConnection().connect(instance, new Pos(5, 40, 0));

        consequence(slender, 0.99D).apply(survivor);

        assertEquals(List.of(survivor), punished);
        assertTrue(revealed.isEmpty());
        assertEquals(1, survivor.getTag(StagedCatchConsequence.CATCHES));
    }

    @Test
    @DisplayName("The second catch gives the survivor away")
    void secondCatchBetrays(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        Player slender = env.createConnection().connect(instance, new Pos(5, 40, 0));
        StagedCatchConsequence consequence = consequence(slender, 0.99D);

        consequence.apply(survivor);
        consequence.apply(survivor);

        assertEquals(1, revealed.size());
        assertEquals(survivor, revealed.getFirst()[0]);
        assertEquals(slender, revealed.getFirst()[1]);
        assertNull(survivor.getTag(StagedCatchConsequence.CATCHES), "the count starts over");
    }

    @Test
    @DisplayName("A lucky draw gives the survivor away early")
    void luckyDrawBetraysEarly(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        Player slender = env.createConnection().connect(instance, new Pos(5, 40, 0));

        consequence(slender, 0.0D).apply(survivor);

        assertEquals(1, revealed.size());
    }

    @Test
    @DisplayName("Without a slender nobody is given away")
    void withoutASlender(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        StagedCatchConsequence consequence = consequence(null, 0.0D);

        consequence.apply(survivor);
        consequence.apply(survivor);

        assertTrue(revealed.isEmpty());
        assertEquals(2, survivor.getTag(StagedCatchConsequence.CATCHES));
    }

    @Test
    @DisplayName("Cleaning up forgets the count and cleans the parts")
    void cleanUpForgets(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        StagedCatchConsequence consequence = consequence(null, 0.99D);
        consequence.apply(survivor);

        consequence.cleanUp();

        assertNull(survivor.getTag(StagedCatchConsequence.CATCHES));
        assertEquals(2, cleanUps);
    }

    private static RandomGenerator always(double value) {
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                return 0L;
            }

            @Override
            public double nextDouble() {
                return value;
            }
        };
    }
}
