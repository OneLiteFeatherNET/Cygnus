package net.onelitefeather.cygnus.disclaimer;

import net.kyori.adventure.key.Key;
import net.minestom.server.dialog.Dialog;
import net.minestom.server.dialog.DialogAction;
import net.minestom.server.dialog.DialogAfterAction;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.common.ShowDialogPacket;
import net.minestom.server.network.packet.server.play.SetTitleTextPacket;
import net.minestom.server.network.packet.server.play.SoundEffectPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class EpilepsyDisclaimerTest extends CygnusPlayerTestBase {

    @Test
    void testShowToOpensANoticeDialogTheButtonCanBeAnsweredFrom(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<ShowDialogPacket> dialogs = connection.trackIncoming(ShowDialogPacket.class);

        new EpilepsyDisclaimer().showTo(player);

        dialogs.assertSingle(packet -> {
            Dialog.Notice notice = assertInstanceOf(Dialog.Notice.class, packet.dialog(),
                    "a warning has nothing to decide, so it must not come as a confirmation");
            DialogAction.Custom action = assertInstanceOf(DialogAction.Custom.class, notice.action().action(),
                    "the button has to report back to the server, otherwise the title never follows");
            assertEquals(EpilepsyDisclaimer.ACKNOWLEDGE_KEY, action.key(),
                    "the button has to send the id the listener waits for");
            assertFalse(notice.metadata().canCloseWithEscape(),
                    "a warning that can be dismissed with a keypress before it is read is not a warning");
            assertEquals(DialogAfterAction.CLOSE, notice.metadata().afterAction(),
                    "the dialog has to close once the player took note of it");
        });

        env.destroyInstance(instance, true);
    }

    @Test
    void testTheWarningIsShownOnTheFirstSpawn(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        EventNode<Event> node = EventNode.all("epilepsy-disclaimer-test");
        env.process().eventHandler().addChild(node);
        new EpilepsyDisclaimer().registerListener(node);
        TestConnection connection = env.createConnection();
        Collector<ShowDialogPacket> dialogs = connection.trackIncoming(ShowDialogPacket.class);

        connection.connect(instance);

        dialogs.assertSingle();

        env.process().eventHandler().removeChild(node);
        env.destroyInstance(instance, true);
    }

    @Test
    void testTheButtonPlaysTheWarningBackAsATitle(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        EpilepsyDisclaimer disclaimer = new EpilepsyDisclaimer();
        Collector<SetTitleTextPacket> titles = connection.trackIncoming(SetTitleTextPacket.class);
        Collector<SoundEffectPacket> sounds = connection.trackIncoming(SoundEffectPacket.class);

        disclaimer.handleClick(new PlayerCustomClickEvent(player, EpilepsyDisclaimer.ACKNOWLEDGE_KEY, null));

        titles.assertSingle();
        sounds.assertSingle();

        env.destroyInstance(instance, true);
    }

    @Test
    void testAClickFromAnotherDialogIsLeftAlone(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        EpilepsyDisclaimer disclaimer = new EpilepsyDisclaimer();
        Collector<SetTitleTextPacket> titles = connection.trackIncoming(SetTitleTextPacket.class);

        disclaimer.handleClick(new PlayerCustomClickEvent(player, Key.key("cygnus", "something/else"), null));

        assertEquals(List.of(), titles.collect(), "another dialog's button must not trigger this warning");

        env.destroyInstance(instance, true);
    }
}
