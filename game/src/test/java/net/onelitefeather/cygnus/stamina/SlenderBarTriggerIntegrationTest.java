package net.onelitefeather.cygnus.stamina;

import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.onelitefeather.cygnus.utils.ViewRuleUpdater;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test verifying that {@link SlenderBarTrigger} switches the slender's visibility correctly.
 * DRAINING (actively using the ability) makes the slender visible, REGENERATING (recovering) hides them.
 */
class SlenderBarTriggerIntegrationTest extends CygnusPlayerTestBase {

    @Test
    void testTriggerMakesPlayerVisibleWhenActivatingDraining(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer player = (CygnusPlayer) env.createPlayer(instance);
        player.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);

        SlenderBar slenderBar = (SlenderBar) StaminaFactory.createSlenderStamina(player);
        slenderBar.start();

        SlenderBarTrigger trigger = new SlenderBarTrigger(() -> slenderBar, ignored -> {
        });
        trigger.trigger(player);

        assertFalse(ViewRuleUpdater.isHidden(player), "player should be visible right after activating draining");

        slenderBar.stop();
        env.destroyInstance(instance, true);
    }

    @Test
    void testRapidTriggerIsBlockedByCooldownAndGivesFeedback(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        CygnusPlayer player = (CygnusPlayer) connection.connect(instance);
        player.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);

        SlenderBar slenderBar = (SlenderBar) StaminaFactory.createSlenderStamina(player);
        slenderBar.start();

        SlenderBarTrigger trigger = new SlenderBarTrigger(() -> slenderBar, ignored -> {
        });
        trigger.trigger(player); // READY -> DRAINING

        Collector<ServerPacket> collector = connection.trackIncoming();
        trigger.trigger(player); // immediately again, still within the spam cooldown

        assertFalse(ViewRuleUpdater.isHidden(player), "the second, cooldown-blocked trigger must not toggle the status again");
        assertTrue(soundWasSent(collector), "player should get audible feedback that the trigger is on cooldown");

        slenderBar.stop();
        env.destroyInstance(instance, true);
    }

    @Test
    void testTriggerGivesFeedbackWhenBlockedByInsufficientRegeneration(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        CygnusPlayer player = (CygnusPlayer) connection.connect(instance);
        player.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);

        SlenderBar slenderBar = (SlenderBar) StaminaFactory.createSlenderStamina(player);
        slenderBar.start();
        slenderBar.changeStatus(); // READY -> DRAINING, bypassing the trigger's own cooldown bookkeeping

        // fully drain so the bar auto-switches to REGENERATING while stamina is still low
        for (int i = 0; i < 34; i++) {
            slenderBar.consume();
        }

        SlenderBarTrigger trigger = new SlenderBarTrigger(() -> slenderBar, ignored -> {
        });
        Collector<ServerPacket> collector = connection.trackIncoming();
        trigger.trigger(player);

        assertTrue(ViewRuleUpdater.isHidden(player), "player should stay hidden/blocked instead of toggling back to draining");
        assertTrue(soundWasSent(collector), "player should get audible feedback when blocked by insufficient regeneration");

        slenderBar.stop();
        env.destroyInstance(instance, true);
    }

    private static boolean soundWasSent(Collector<ServerPacket> collector) {
        return collector.collect().stream().anyMatch(packet -> packet.getClass().getSimpleName().contains("Sound"));
    }
}
