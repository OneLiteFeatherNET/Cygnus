package net.onelitefeather.cygnus.view;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.BossBarPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.page.PageProvider;
import net.onelitefeather.cygnus.listener.view.ViewUpdateListener;
import net.onelitefeather.cygnus.view.event.ViewUpdateEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MicrotusExtension.class)
class GameViewIntegrationTest {

    @Test
    void testViewUpdate(Env env) {
        Instance instance = env.createEmptyInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        GameView gameView = new GameViewImpl();
        PageProvider pageProvider = new PageProvider(() -> {
        });

        env.process().eventHandler().addListener(ViewUpdateEvent.class, new ViewUpdateListener(gameView, pageProvider));

        Collector<BossBarPacket> barCollector = connection.trackIncoming(BossBarPacket.class);

        gameView.addPlayer(player);

        barCollector.assertSingle();

        ViewUpdateEvent updateEvent = new ViewUpdateEvent(100);

        EventFilter<ViewUpdateEvent, ViewUpdateEvent> filter = EventFilter.from(
                ViewUpdateEvent.class,
                ViewUpdateEvent.class,
                e -> e
        );
        Collector<ViewUpdateEvent> eventCollector = env.trackEvent(ViewUpdateEvent.class, filter, updateEvent);
        Collector<BossBarPacket> secondBarCollector = connection.trackIncoming(BossBarPacket.class);

        env.process().eventHandler().call(updateEvent);

        env.tick();

        eventCollector.assertSingle();
        eventCollector.assertSingle(event -> {
            assertEquals(updateEvent.ticks(), event.ticks());
        });

        secondBarCollector.assertSingle(bossBarPacket -> {
            assertInstanceOf(BossBarPacket.UpdateTitleAction.class, bossBarPacket.action());

            BossBarPacket.UpdateTitleAction updateTitle = ((BossBarPacket.UpdateTitleAction) bossBarPacket.action());
            assertEquals(1, updateTitle.components().size());
            Component component = updateTitle.components().stream().findAny().orElse(Component.empty());
            assertNotEquals(component, Component.empty());

            String content = PlainTextComponentSerializer.plainText().serialize(component);
            assertTrue(content.contains("Time: 01:40"));
        });

        env.destroyInstance(instance, true);
    }
}
