package net.onelitefeather.cygnus.lure;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.potion.PotionEffect;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.LureConfig;
import net.onelitefeather.cygnus.creek.CreekRespite;
import net.onelitefeather.cygnus.creek.consequence.SlenderReveal;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LureAftermathIntegrationTest extends CygnusPlayerTestBase {

    private final List<String> fear = new ArrayList<>();
    private final List<UUID> revealed = new ArrayList<>();
    private final List<String> cleanUps = new ArrayList<>();
    private final CreekRespite respite = new CreekRespite();
    private @Nullable Player slender;
    private LureOutcome outcome = LureOutcome.NOTHING;

    @Test
    @DisplayName("A strike raises the fear and spares the survivor from the creek")
    void strikeScaresAndSpares(Env env) {
        Player survivor = join(env);

        aftermath().struck(survivor, 1_000L);

        assertEquals(List.of(survivor.getUuid() + ":0.3"), this.fear);
        assertTrue(this.respite.spares(survivor.getUuid(), 1_000L + 59_999L));
        assertFalse(this.respite.spares(survivor.getUuid(), 1_000L + 60_000L));
    }

    @Test
    @DisplayName("Slowness follows up as a potion effect")
    void slownessFollowsUp(Env env) {
        Player survivor = join(env);
        this.outcome = LureOutcome.SLOWNESS;

        aftermath().followUp(survivor);

        assertTrue(survivor.hasEffect(PotionEffect.SLOWNESS));
    }

    @Test
    @DisplayName("Blindness follows up as a potion effect")
    void blindnessFollowsUp(Env env) {
        Player survivor = join(env);
        this.outcome = LureOutcome.BLINDNESS;

        aftermath().followUp(survivor);

        assertTrue(survivor.hasEffect(PotionEffect.BLINDNESS));
    }

    @Test
    @DisplayName("A betrayal shows the survivor to the slender")
    void betrayalRevealsTheSurvivor(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        this.slender = env.createConnection().connect(instance, new Pos(-300.5, 40, 0.5));
        this.outcome = LureOutcome.BETRAYAL;

        aftermath().followUp(survivor);

        assertEquals(List.of(survivor.getUuid()), this.revealed);
    }

    @Test
    @DisplayName("Without a slender a betrayal gives nobody away")
    void betrayalWithoutSlenderDoesNothing(Env env) {
        Player survivor = join(env);
        this.outcome = LureOutcome.BETRAYAL;

        aftermath().followUp(survivor);

        assertTrue(this.revealed.isEmpty());
    }

    @Test
    @DisplayName("Cleaning up forgets every breather and ends every reveal")
    void cleanUpClearsRespiteAndReveals() {
        UUID spared = UUID.randomUUID();
        this.respite.spare(spared, Long.MAX_VALUE);

        aftermath().cleanUp();

        assertFalse(this.respite.spares(spared, 0L));
        assertEquals(List.of("reveal"), this.cleanUps);
    }

    private LureAftermath aftermath() {
        SlenderReveal reveal = new SlenderReveal() {
            @Override
            public void reveal(Player survivor, Player slender) {
                revealed.add(survivor.getUuid());
            }

            @Override
            public void cleanUp() {
                cleanUps.add("reveal");
            }
        };
        return new LureAftermath(LureConfig.DEFAULT, (id, gain) -> this.fear.add(id + ":" + gain), this.respite,
                () -> this.slender, reveal, () -> this.outcome);
    }

    private static Player join(Env env) {
        return env.createConnection().connect(env.createFlatInstance(), new Pos(0, 40, 0));
    }
}
