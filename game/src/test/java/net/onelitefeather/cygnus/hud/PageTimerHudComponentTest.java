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
import net.onelitefeather.cygnus.common.text.SpaceFont;
import net.theevilreaper.aves.util.Strings;
import net.theevilreaper.aves.util.TimeFormat;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageTimerHudComponentTest extends CygnusPlayerTestBase {

    private static final TextColor MARKER_PAGES = TextColor.color(254, 254, 250);
    private static final TextColor MARKER_TIMER = TextColor.color(254, 254, 249);
    private static final char ICON_PAGE = '\ue102';
    private static final char ICON_CLOCK = '\ue101';
    private static final int PADDING_PX = 2;
    private static final int GAP_PX = 4;

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

        PageTimerHudComponent component = new PageTimerHudComponent();
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

        PageTimerHudComponent component = new PageTimerHudComponent();
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

        PageTimerHudComponent component = new PageTimerHudComponent();
        component.addPlayer(player);

        Component pageStatus = Component.text("3", NamedTextColor.GREEN)
                .append(Component.space())
                .append(Component.text("/", NamedTextColor.GRAY))
                .append(Component.space())
                .append(Component.text("10", NamedTextColor.RED));
        int ticks = 100;

        Collector<BossBarPacket> updateCollector = connection.trackIncoming(BossBarPacket.class);
        component.update(ticks, pageStatus);

        Component time = Component.text(Strings.getTimeString(TimeFormat.MM_SS, ticks));
        Component expected = HudSegment.segment(ICON_PAGE, pageStatus, PADDING_PX, MARKER_PAGES)
                .append(SpaceFont.positive(GAP_PX))
                .append(HudSegment.segment(ICON_CLOCK, time, PADDING_PX, MARKER_TIMER));

        updateCollector.assertSingle(packet -> {
            assertInstanceOf(BossBarPacket.UpdateTitleAction.class, packet.action());
            BossBarPacket.UpdateTitleAction updateTitle = (BossBarPacket.UpdateTitleAction) packet.action();
            assertEquals(expected, updateTitle.title());
        });
    }

    @Test
    void renderIsNoOpAndHideMarksInvisible() {
        PageTimerHudComponent component = new PageTimerHudComponent();

        assertTrue(component.isVisible());
        component.render();
        assertTrue(component.isVisible());

        component.hide();
        assertFalse(component.isVisible());
    }
}
