package net.onelitefeather.cygnus.hud;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.BossBarPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.page.PageProvider;
import net.onelitefeather.cygnus.common.text.TextWidth;
import net.onelitefeather.cygnus.listener.view.ViewUpdateListener;
import net.onelitefeather.cygnus.view.event.ViewUpdateEvent;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MicrotusExtension.class)
class PageTimerHudComponentIntegrationTest {

    private static final Key MISC_FONT = Key.key("cygnus", "misc");

    @Disabled("Investigate why this test is broken")
    @Test
    void testViewUpdate(Env env) {
        Instance instance = env.createEmptyInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        PageTimerHudComponent pageTimerHudComponent = new PageTimerHudComponent();
        PageCountHudComponent pageCountHudComponent = new PageCountHudComponent();
        PageProvider pageProvider = new PageProvider();

        env.process().eventHandler().addListener(ViewUpdateEvent.class,
                new ViewUpdateListener(pageTimerHudComponent, pageCountHudComponent, pageProvider));

        Collector<BossBarPacket> barCollector = connection.trackIncoming(BossBarPacket.class);

        pageTimerHudComponent.addPlayer(player);

        barCollector.assertSingle(packet -> assertInstanceOf(BossBarPacket.AddAction.class, packet.action()));

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
            Component component = updateTitle.title();
            assertNotEquals(Component.empty(), component);

            // The new rendering is pixel-segment based (bitmap font glyphs + space:default offset
            // glyphs), not literal text, so instead of matching a "Time: 01:40" string we assert
            // the component actually carries rendered content (a non-zero measured pixel width)
            // and that it uses the cygnus:misc bar-glyph font somewhere in its tree.
            assertTrue(TextWidth.widthOf(component) > 0, "Expected the combined bar to have a non-zero measured width");
            assertTrue(usesFont(component, MISC_FONT), "Expected the combined bar to use the cygnus:misc font somewhere in its tree");
        });

        env.destroyInstance(instance, true);
    }

    private static boolean usesFont(Component component, Key font) {
        if (font.equals(component.style().font())) return true;
        for (Component child : component.children()) {
            if (usesFont(child, font)) return true;
        }
        return false;
    }
}
