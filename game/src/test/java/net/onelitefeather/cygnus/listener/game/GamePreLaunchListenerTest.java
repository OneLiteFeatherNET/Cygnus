package net.onelitefeather.cygnus.listener.game;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.instance.Instance;
import net.minestom.server.utils.Direction;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.config.EightPagesConfig;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.event.GamePreLaunchEvent;
import net.onelitefeather.cygnus.common.page.PageProvider;
import net.onelitefeather.cygnus.common.page.PageResource;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MicrotusExtension.class)
class GamePreLaunchListenerTest {

    /**
     * Every test gets its own environment, so the player count the listener reads is exactly the
     * players a test creates. The class therefore does not share the environment of
     * {@code CygnusPlayerTestBase}, and makes its players Cygnus players here instead.
     */
    @BeforeEach
    void cygnusPlayers(Env env) {
        env.process().connection().setPlayerProvider((connection, gameProfile) -> new CygnusPlayer(connection, gameProfile, null));
    }

    @Test
    void testPreLaunchSetsThePageAmountAndCollectsTheStartPages(@NotNull Env env) {
        PageProvider pageProvider = loadedProvider();

        launch(env, pageProvider, EightPagesConfig.DEFAULT);

        int pages = pageProvider.getMaxPageAmount();
        assertTrue(pages >= 8 && pages <= 10, "without players the round is an Eight Pages round, was " + pages);
        assertEquals(GameConfig.MIN_ACTIVE_PAGE_COUNT, pageProvider.interactablePages().size(),
                "without any players the round starts with the minimum of active pages");
        assertTrue(pageProvider.interactablePages().stream().allMatch(page -> page.getInstance() == null),
                "the pages are only collected here, the round start places them");
    }

    @Test
    void testTwoSurvivorsGetTheConfiguredBonusAsPoints(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer first = (CygnusPlayer) env.createPlayer(instance);
        env.createPlayer(instance);
        env.createPlayer(instance);

        launch(env, loadedProvider(), new EightPagesConfig(3, 30, 20, 10));

        assertEquals(120.0D, first.vitality().points(), 1.0E-9);
        assertEquals(24.0D, first.getAttributeValue(Attribute.MAX_HEALTH), 1.0E-9);

        env.destroyInstance(instance, true);
    }

    @Test
    void testFourSurvivorsGetNoBonus(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer first = (CygnusPlayer) env.createPlayer(instance);
        for (int i = 0; i < 4; i++) {
            env.createPlayer(instance);
        }

        launch(env, loadedProvider(), EightPagesConfig.DEFAULT);

        assertEquals(100.0D, first.vitality().points(), 1.0E-9);
        assertEquals(20.0F, first.getHealth());

        env.destroyInstance(instance, true);
    }

    @Test
    void testThreeSurvivorsNeedEightToTenPages(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        for (int i = 0; i < 4; i++) {
            env.createPlayer(instance);
        }
        PageProvider pageProvider = loadedProvider();

        launch(env, pageProvider, EightPagesConfig.DEFAULT);

        assertTrue(pageProvider.getMaxPageAmount() <= 10);

        env.destroyInstance(instance, true);
    }

    @Test
    void testFourSurvivorsNeedTwelveToSixteenPages(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        for (int i = 0; i < 5; i++) {
            env.createPlayer(instance);
        }
        PageProvider pageProvider = loadedProvider();

        launch(env, pageProvider, EightPagesConfig.DEFAULT);

        int pages = pageProvider.getMaxPageAmount();
        assertTrue(pages >= 12 && pages <= 16, "was " + pages);

        env.destroyInstance(instance, true);
    }

    /**
     * Registers the listener on the server's event handler, as the game does, and fires the event
     * through it. The listener sits in its own node, which is removed again afterwards.
     */
    private static void launch(Env env, PageProvider pageProvider, EightPagesConfig eightPages) {
        EventNode<Event> node = EventNode.all("pre-launch-test");
        node.addListener(GamePreLaunchEvent.class, new GamePreLaunchListener(pageProvider, eightPages));
        GlobalEventHandler handler = env.process().eventHandler();
        handler.addChild(node);
        try {
            handler.call(new GamePreLaunchEvent());
        } finally {
            handler.removeChild(node);
        }
    }

    private static PageProvider loadedProvider() {
        PageProvider pageProvider = new PageProvider();
        pageProvider.loadPageData(
                IntStream.range(0, GameConfig.MIN_ACTIVE_PAGE_COUNT * 2)
                        .mapToObj(i -> new PageResource(new Pos(i, 40, 0), Direction.NORTH))
                        .collect(Collectors.toSet())
        );
        return pageProvider;
    }
}
