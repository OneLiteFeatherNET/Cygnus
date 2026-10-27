package net.onelitefeather.cygnus.tutorial;

import net.kyori.adventure.key.Key;
import net.minestom.server.dialog.Dialog;
import net.minestom.server.dialog.DialogAction;
import net.minestom.server.dialog.DialogActionButton;
import net.minestom.server.dialog.DialogAfterAction;
import net.minestom.server.dialog.DialogBody;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.server.common.ShowDialogPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.disclaimer.EpilepsyDisclaimer;
import net.onelitefeather.cygnus.utils.Items;
import net.onelitefeather.pica.dialog.meta.DialogMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the tutorial pages as the client receives them: which buttons each page carries, where
 * they lead, how much text a page holds and which items the item pages show.
 */
@ExtendWith(MicrotusExtension.class)
class TutorialPagesTest {

    private static final List<Key> SURVIVOR_CHAPTER = List.of(TutorialPages.SURVIVOR_PAGES_KEY,
            TutorialPages.SURVIVOR_DANGER_KEY, TutorialPages.SURVIVOR_FEAR_KEY, TutorialPages.SURVIVOR_ITEMS_KEY);
    private static final List<Key> SLENDER_CHAPTER = List.of(TutorialPages.SLENDER_HUNT_KEY,
            TutorialPages.SLENDER_CREEK_KEY, TutorialPages.SLENDER_ITEMS_KEY);
    private static final List<Key> RULE_PAGES = List.of(TutorialPages.SURVIVOR_PAGES_KEY,
            TutorialPages.SURVIVOR_DANGER_KEY, TutorialPages.SURVIVOR_FEAR_KEY,
            TutorialPages.SLENDER_HUNT_KEY, TutorialPages.SLENDER_CREEK_KEY);
    private static final List<Key> PAGE_KEYS = Stream.of(List.of(TutorialPages.INTRO_KEY), SURVIVOR_CHAPTER,
            SLENDER_CHAPTER).flatMap(List::stream).toList();

    private TutorialPages pages;
    private Env env;
    private Instance instance;

    @BeforeEach
    void setUp(Env env) {
        this.env = env;
        this.instance = env.createFlatInstance();
        this.pages = new TutorialPages();
    }

    private Dialog.MultiAction received(Key key) {
        TestConnection connection = this.env.createConnection();
        Player player = connection.connect(this.instance);
        Collector<ShowDialogPacket> dialogs = connection.trackIncoming(ShowDialogPacket.class);
        var page = this.pages.page(key);
        assertNotNull(page, "every page key has a page");
        page.open(player);
        List<ShowDialogPacket> packets = dialogs.collect();
        assertEquals(1, packets.size(), "opening a page sends exactly one dialog");
        return assertInstanceOf(Dialog.MultiAction.class, packets.getFirst().dialog(),
                "only a multi-action dialog carries buttons of its own");
    }

    private static Key keyOf(DialogActionButton button) {
        return assertInstanceOf(DialogAction.Custom.class, button.action(),
                "a button has to report back, otherwise the page never turns").key();
    }

    private static List<Key> buttonKeys(Dialog.MultiAction dialog) {
        return dialog.actions().stream().map(TutorialPagesTest::keyOf).toList();
    }

    private static List<ItemStack> shownItems(Dialog.MultiAction dialog) {
        return dialog.metadata().body().stream()
                .filter(DialogBody.Item.class::isInstance)
                .map(body -> ((DialogBody.Item) body).itemStack())
                .toList();
    }

    private static long paragraphs(Dialog.MultiAction dialog) {
        return dialog.metadata().body().stream()
                .filter(DialogBody.PlainMessage.class::isInstance)
                .filter(body -> !body.equals(DialogMeta.EMPTY))
                .count();
    }

    @Test
    @DisplayName("The introduction lists both roles one below the other")
    void introOffersBothRoles() {
        Dialog.MultiAction intro = received(TutorialPages.INTRO_KEY);

        assertEquals(List.of(TutorialPages.SURVIVOR_PAGES_KEY, TutorialPages.SLENDER_HUNT_KEY), buttonKeys(intro));
        assertEquals(1, intro.columns(), "the menu lists its buttons one below the other");
    }

    @Test
    @DisplayName("Every page carries close in its footer, so escape closes the tutorial as well")
    void everyPageClosesFromTheFooter() {
        for (Key key : PAGE_KEYS) {
            Dialog.MultiAction page = received(key);

            assertNotNull(page.exitAction(), "the footer holds the way out: " + key);
            assertEquals(TutorialPages.CLOSE_KEY, keyOf(page.exitAction()), "escape runs the footer button: " + key);
        }
    }

    @Test
    @DisplayName("Every page turns without closing and can be left with escape")
    void everyPageTurnsWithoutClosing() {
        for (Key key : PAGE_KEYS) {
            Dialog.MultiAction page = received(key);

            assertEquals(DialogAfterAction.NONE, page.metadata().afterAction(),
                    "closing first would flash the world before the next page: " + key);
            assertTrue(page.metadata().canCloseWithEscape(), "nothing here has to be read: " + key);
            assertFalse(page.metadata().pause(), "the lobby keeps running behind the tutorial: " + key);
        }
    }

    @Test
    @DisplayName("Each chapter turns like a book: back to the page before, on to the next, and from the last to the start")
    void chaptersTurnLikeABook() {
        for (List<Key> chapter : List.of(SURVIVOR_CHAPTER, SLENDER_CHAPTER)) {
            for (int i = 0; i < chapter.size(); i++) {
                Key back = i == 0 ? TutorialPages.INTRO_KEY : chapter.get(i - 1);
                Key next = i == chapter.size() - 1 ? TutorialPages.INTRO_KEY : chapter.get(i + 1);
                Dialog.MultiAction page = received(chapter.get(i));

                assertEquals(List.of(back, next), buttonKeys(page), "wrong buttons on " + chapter.get(i));
                assertEquals(2, page.columns(), "back and next belong side by side: " + chapter.get(i));
            }
        }
    }

    @Test
    @DisplayName("A rule page explains two points, so it never has to scroll")
    void rulePagesHoldTwoPoints() {
        for (Key key : RULE_PAGES) {
            assertEquals(2, paragraphs(received(key)), "wrong number of points on " + key);
        }
    }

    @Test
    @DisplayName("Every button leads to a page or closes the tutorial")
    void everyButtonLeadsToAPageOrCloses() {
        for (Key key : PAGE_KEYS) {
            for (Key target : buttonKeys(received(key))) {
                assertTrue(target.equals(TutorialPages.CLOSE_KEY) || this.pages.page(target) != null,
                        "a button on " + key + " leads nowhere: " + target);
            }
        }
    }

    @Test
    @DisplayName("The item pages show the very items the round hands out")
    void itemPagesShowTheRoundItems() {
        assertEquals(List.of(Items.pageReveal(), Items.painkillers()),
                shownItems(received(TutorialPages.SURVIVOR_ITEMS_KEY)));
        assertEquals(List.of(Items.slenderEye(), Items.tracker(), Items.possession()),
                shownItems(received(TutorialPages.SLENDER_ITEMS_KEY)));
    }

    @Test
    @DisplayName("Close and keys of other dialogs have no page")
    void unknownKeysHaveNoPage() {
        assertNull(this.pages.page(TutorialPages.CLOSE_KEY));
        assertNull(this.pages.page(EpilepsyDisclaimer.ACKNOWLEDGE_KEY));
    }
}
