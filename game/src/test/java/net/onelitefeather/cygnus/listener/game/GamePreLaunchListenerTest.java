package net.onelitefeather.cygnus.listener.game;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.instance.Instance;
import net.minestom.server.utils.Direction;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.event.GamePreLaunchEvent;
import net.onelitefeather.cygnus.common.page.PageProvider;
import net.onelitefeather.cygnus.common.page.PageResource;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MicrotusExtension.class)
class GamePreLaunchListenerTest {

    @Test
    void testPreLaunchSetsThePageAmountAndCollectsTheStartPages(@NotNull Env env) {
        PageProvider pageProvider = loadedProvider();

        new GamePreLaunchListener(pageProvider).accept(new GamePreLaunchEvent());

        assertTrue(pageProvider.getMaxPageAmount() >= GameConfig.MIN_PAGE_COUNT);
        assertEquals(GameConfig.MIN_ACTIVE_PAGE_COUNT, pageProvider.interactablePages().size(),
                "without any players the round starts with the minimum of active pages");
        assertTrue(pageProvider.interactablePages().stream().allMatch(page -> page.getInstance() == null),
                "the pages are only collected here, the round start places them");
    }

    @Test
    void testTwoPlayersAlwaysGetTheExtraHearts(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createPlayer(instance);
        env.createPlayer(instance);

        new GamePreLaunchListener(loadedProvider()).accept(new GamePreLaunchEvent());

        // The page count carries a random jitter, so the bonus must not depend on it
        assertTrue(survivor.getAttributeValue(Attribute.MAX_HEALTH) > 20.0D,
                "a small lobby must always get the extra hearts");

        env.destroyInstance(instance, true);
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
