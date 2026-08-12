package net.onelitefeather.cygnus.hud;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.BossBarPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.text.HudSegment;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageCountHudComponentTest extends CygnusPlayerTestBase {

    private static final TextColor MARKER_PAGES = TextColor.color(254, 254, 250);
    private static final char ICON_PAGE = '\ue102';
    private static final int PADDING_PX = 2;

    private Instance instance;

    @AfterEach
    void teardown(@NotNull Env env) {
        if (instance != null) {
            env.destroyInstance(instance, true);
            instance = null;
        }
    }

    @Test
    void addPlayerTracksPlayerAndShowsBossBar(@NotNull Env env) {
        instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);

        PageCountHudComponent component = new PageCountHudComponent();
        Collector<BossBarPacket> addCollector = connection.trackIncoming(BossBarPacket.class);

        component.addPlayer(player);

        assertTrue(component.getPlayers().contains(player));
        addCollector.assertSingle(packet -> assertInstanceOf(BossBarPacket.AddAction.class, packet.action()));
    }

    @Test
    void removePlayerUntracksPlayerAndHidesBossBar(@NotNull Env env) {
        instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);

        PageCountHudComponent component = new PageCountHudComponent();
        component.addPlayer(player);

        Collector<BossBarPacket> removeCollector = connection.trackIncoming(BossBarPacket.class);
        component.removePlayer(player);

        assertFalse(component.getPlayers().contains(player));
        removeCollector.assertSingle(packet -> assertInstanceOf(BossBarPacket.RemoveAction.class, packet.action()));
    }

    @Test
    void updateSetsExpectedBossBarName(@NotNull Env env) {
        instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);

        PageCountHudComponent component = new PageCountHudComponent();
        component.addPlayer(player);

        Component pageStatus = Component.text("3", NamedTextColor.GREEN)
                .append(Component.space())
                .append(Component.text("/", NamedTextColor.GRAY))
                .append(Component.space())
                .append(Component.text("10", NamedTextColor.RED));

        Collector<BossBarPacket> updateCollector = connection.trackIncoming(BossBarPacket.class);
        component.update(pageStatus);

        Component expected = HudSegment.segment(ICON_PAGE, pageStatus, PADDING_PX, MARKER_PAGES);
        updateCollector.assertSingle(packet -> {
            assertInstanceOf(BossBarPacket.UpdateTitleAction.class, packet.action());
            BossBarPacket.UpdateTitleAction updateTitle = (BossBarPacket.UpdateTitleAction) packet.action();
            assertEquals(expected, updateTitle.title());
        });
    }

    @Test
    void renderIsNoOpAndHideMarksInvisible() {
        PageCountHudComponent component = new PageCountHudComponent();

        assertTrue(component.isVisible());
        component.render();
        assertTrue(component.isVisible());

        component.hide();
        assertFalse(component.isVisible());
    }
}
