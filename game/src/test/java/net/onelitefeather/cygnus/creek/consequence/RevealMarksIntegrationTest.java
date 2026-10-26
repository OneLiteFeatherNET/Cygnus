package net.onelitefeather.cygnus.creek.consequence;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.creek.state.Contexts;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RevealMarksIntegrationTest extends CygnusPlayerTestBase {

    private final List<Player> revealed = new ArrayList<>();
    private final AtomicLong clock = new AtomicLong();

    private RevealMarks marks(@Nullable Player slender, double roll) {
        return new RevealMarks((survivor, _) -> this.revealed.add(survivor), () -> slender, this.clock::get,
                Contexts.rolling(roll));
    }

    @Test
    @DisplayName("The end of a stare marks with a chance of one half")
    void stareMarksAtHalf(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        Player slender = env.createConnection().connect(instance, new Pos(5, 40, 0));

        this.marks(slender, 0.5D).stared(survivor);
        assertTrue(this.revealed.isEmpty(), "a roll of one half misses");

        this.marks(slender, 0.49D).stared(survivor);
        assertEquals(List.of(survivor), this.revealed);
    }

    @Test
    @DisplayName("A look at the stalking creek marks with a chance of one quarter")
    void spotMarksAtQuarter(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        Player slender = env.createConnection().connect(instance, new Pos(5, 40, 0));

        this.marks(slender, 0.25D).spotted(survivor);
        assertTrue(this.revealed.isEmpty(), "a roll of one quarter misses");

        this.marks(slender, 0.24D).spotted(survivor);
        assertEquals(List.of(survivor), this.revealed);
    }

    @Test
    @DisplayName("A marked survivor is safe from the next mark for 20 seconds, others are not")
    void cooldownIsPerSurvivor(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        Player other = env.createConnection().connect(instance, new Pos(10, 40, 0));
        Player slender = env.createConnection().connect(instance, new Pos(5, 40, 0));
        RevealMarks marks = this.marks(slender, 0.0D);

        marks.stared(survivor);
        this.clock.set(RevealMarks.COOLDOWN_MILLIS - 1L);
        marks.spotted(survivor);
        marks.stared(other);
        this.clock.set(RevealMarks.COOLDOWN_MILLIS);
        marks.spotted(survivor);

        assertEquals(List.of(survivor, other, survivor), this.revealed);
    }

    @Test
    @DisplayName("Cleaning up forgets the breathers")
    void cleanUpForgetsTheCooldown(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        Player slender = env.createConnection().connect(instance, new Pos(5, 40, 0));
        RevealMarks marks = this.marks(slender, 0.0D);

        marks.stared(survivor);
        marks.cleanUp();
        marks.stared(survivor);

        assertEquals(List.of(survivor, survivor), this.revealed);
    }

    @Test
    @DisplayName("Without a slender nobody is marked, and no breather starts")
    void noSlenderNoMark(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));

        this.marks(null, 0.0D).stared(survivor);

        assertTrue(this.revealed.isEmpty());
    }
}
