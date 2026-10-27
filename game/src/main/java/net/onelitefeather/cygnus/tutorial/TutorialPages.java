package net.onelitefeather.cygnus.tutorial;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponents;
import net.minestom.server.dialog.DialogAction;
import net.minestom.server.dialog.DialogAfterAction;
import net.minestom.server.item.ItemStack;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.utils.Items;
import net.onelitefeather.pica.dialog.DialogTemplate;
import net.onelitefeather.pica.dialog.action.ActionButton;
import net.onelitefeather.pica.dialog.meta.DialogMeta;
import net.onelitefeather.pica.dialog.type.DialogType;
import net.onelitefeather.pica.dialog.type.MultiActionDialog;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The pages of the lobby tutorial: the introduction, and for each role a chapter of rule pages
 * followed by one page on its items.
 *
 * <p>Every button reports a key of its own, and every key but {@link #CLOSE_KEY} names the page it
 * leads to, so turning a page needs nothing but the key the client sends back. The pages are built
 * once and shared by every player.</p>
 *
 * <p>A chapter turns like a book: the left button goes back one page, the right one goes on, and on
 * the last page of a chapter it leads back to the introduction. A rule page explains two points, so
 * it never has to scroll and its buttons stay in sight.</p>
 *
 * <p>Close sits in the footer of every page, where the client keeps it at the bottom. The footer
 * button is also what escape runs, so escape closes the tutorial through the same key.</p>
 *
 * <p>All pages stay open after a click. The next page then replaces the current one directly;
 * closing first would show the world for a moment in between.</p>
 *
 * <p>The item pages show the very stacks the round hands out, so the icon and the coloured name a
 * player learns here are the ones they find in their hotbar later.</p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
final class TutorialPages {

    /** Opens the introduction. */
    static final Key INTRO_KEY = Key.key("cygnus", "tutorial/intro");
    /** Opens the survivors' page on pages and sprinting. */
    static final Key SURVIVOR_PAGES_KEY = Key.key("cygnus", "tutorial/survivor/pages");
    /** Opens the survivors' page on Slenderman and adrenaline. */
    static final Key SURVIVOR_DANGER_KEY = Key.key("cygnus", "tutorial/survivor/danger");
    /** Opens the survivors' page on fear and the creek. */
    static final Key SURVIVOR_FEAR_KEY = Key.key("cygnus", "tutorial/survivor/fear");
    /** Opens the survivors' item page. */
    static final Key SURVIVOR_ITEMS_KEY = Key.key("cygnus", "tutorial/survivor/items");
    /** Opens Slenderman's page on hiding and traces. */
    static final Key SLENDER_HUNT_KEY = Key.key("cygnus", "tutorial/slender/hunt");
    /** Opens Slenderman's page on pages and the creek. */
    static final Key SLENDER_CREEK_KEY = Key.key("cygnus", "tutorial/slender/creek");
    /** Opens Slenderman's item page. */
    static final Key SLENDER_ITEMS_KEY = Key.key("cygnus", "tutorial/slender/items");
    /** Closes the tutorial. It has no page of its own. */
    static final Key CLOSE_KEY = Key.key("cygnus", "tutorial/close");

    private static final String BACK_LABEL = "<gray>← Back";
    private static final String NEXT_LABEL = "<white>Next →";
    private static final String START_LABEL = "<white>To the start";
    private static final String CLOSE_LABEL = "<gray>Close";

    private static final String INTRO_TITLE = "<gold><bold>How to play";
    private static final List<String> INTRO_BODY = List.of(
            "<white>One of you is <bold>Slenderman</bold> and everyone else is a <bold>survivor</bold>.",
            "<green><bold>Survivors</bold> <white>search the map for pages and win if they find enough of them "
                    + "before the time runs out.",
            "<red><bold>Slenderman</bold> <white>wins if nobody is left to find them.",
            "<aqua><bold>The creek</bold> <white>walks the map as well. It is a second figure and answers to "
                    + "nobody.",
            "<gray><italic>Pick a role below to learn more.");

    private static final String SURVIVOR_PAGES_TITLE = "<green><bold>Survivor — Find the pages";
    private static final List<String> SURVIVOR_PAGES_BODY = List.of(
            point("Pages", "Pages are spread across the map but they do not stay forever. A page that is about "
                    + "to vanish starts to chime and the chime gets faster the less time it has left."),
            point("Sprint", "The bar above your hotbar is your breath. Sprinting drains it and it refills once "
                    + "you stop. Run it empty and you cannot sprint again until it has filled up a good part of "
                    + "the way."));

    private static final String SURVIVOR_DANGER_TITLE = "<green><bold>Survivor — Stay alive";
    private static final List<String> SURVIVOR_DANGER_BODY = List.of(
            point("Slenderman", "Most of the time you cannot see him. When he shows himself he is close and "
                    + "hits whoever is near him. Your screen tears apart while he stands in your view."),
            point("Adrenaline", "When he comes close adrenaline lets you run faster for a moment."));

    private static final String SURVIVOR_FEAR_TITLE = "<green><bold>Survivor — Fear";
    private static final List<String> SURVIVOR_FEAR_BODY = List.of(
            point("Fear", "Your level shows your sanity from 100 for calm to 0 for terrified. Pages, the creek "
                    + "and the death of a teammate scare you. Fear wears off again but less and less as the "
                    + "round goes on."),
            point("The creek", "It goes for the most scared. Whoever it catches is dragged to where "
                    + "Slenderman started."));

    private static final String SURVIVOR_ITEMS_TITLE = "<green><bold>Survivor — Your items";

    private static final String SLENDER_HUNT_TITLE = "<red><bold>Slenderman — How to hunt";
    private static final List<String> SLENDER_HUNT_BODY = List.of(
            point("Hidden and shown", "While hidden the survivors cannot see you. Show yourself to hit whoever "
                    + "is near you. While shown you are slower and half blind and cannot sprint. When your bar "
                    + "runs out you vanish again and need time to recover before you can show yourself once "
                    + "more."),
            point("Traces", "While hidden you now and then leave a footprint that only survivors can see."));

    private static final String SLENDER_CREEK_TITLE = "<red><bold>Slenderman — Pages and the creek";
    private static final List<String> SLENDER_CREEK_BODY = List.of(
            point("Pages", "Every page the survivors take scrambles your screen a bit more."),
            point("The creek", "It hunts the survivors on its own. Look through its eyes to find them."));

    private static final String SLENDER_ITEMS_TITLE = "<red><bold>Slenderman — Your items";

    private static final String PAGE_REVEAL_TEXT = "Shows the pages around you on your locator bar for a few "
            + "seconds. It only points roughly to them so the last steps are up to you. Does not work while "
            + "sprinting.";
    private static final String PAINKILLERS_TEXT = "Win back some health over a few seconds. Only once per round "
            + "so save them for when you need them. Fear makes them work slower.";
    private static final String SLENDER_EYE_TEXT = "Hide yourself or show yourself to the survivors.";
    private static final String TRACKER_TEXT = "Reveals where survivors walked around you a little while ago. "
            + "Only you can see the tracks. Locked for a while at the start of a round.";
    private static final String POSSESSION_TEXT = "While hidden look through the eyes of the creek. Survivors "
            + "close to it show on your locator bar. Your body stays behind frozen and visible. Sneak to return.";

    private final Map<Key, DialogTemplate> pages;

    /**
     * Builds all pages.
     */
    TutorialPages() {
        this.pages = new HashMap<>();
        this.pages.put(INTRO_KEY, page(INTRO_KEY, INTRO_TITLE, meta -> addParagraphs(meta, INTRO_BODY))
                .columns(1)
                .action(button("<green>Survivor", SURVIVOR_PAGES_KEY))
                .action(button("<red>Slenderman", SLENDER_HUNT_KEY))
                .build());
        addChapter(List.of(
                new Page(SURVIVOR_PAGES_KEY, page(SURVIVOR_PAGES_KEY, SURVIVOR_PAGES_TITLE,
                        meta -> addParagraphs(meta, SURVIVOR_PAGES_BODY))),
                new Page(SURVIVOR_DANGER_KEY, page(SURVIVOR_DANGER_KEY, SURVIVOR_DANGER_TITLE,
                        meta -> addParagraphs(meta, SURVIVOR_DANGER_BODY))),
                new Page(SURVIVOR_FEAR_KEY, page(SURVIVOR_FEAR_KEY, SURVIVOR_FEAR_TITLE,
                        meta -> addParagraphs(meta, SURVIVOR_FEAR_BODY))),
                new Page(SURVIVOR_ITEMS_KEY, page(SURVIVOR_ITEMS_KEY, SURVIVOR_ITEMS_TITLE, meta -> {
                    addItem(meta, Items.pageReveal(), PAGE_REVEAL_TEXT);
                    addItem(meta, Items.painkillers(), PAINKILLERS_TEXT);
                }))));
        addChapter(List.of(
                new Page(SLENDER_HUNT_KEY, page(SLENDER_HUNT_KEY, SLENDER_HUNT_TITLE,
                        meta -> addParagraphs(meta, SLENDER_HUNT_BODY))),
                new Page(SLENDER_CREEK_KEY, page(SLENDER_CREEK_KEY, SLENDER_CREEK_TITLE,
                        meta -> addParagraphs(meta, SLENDER_CREEK_BODY))),
                new Page(SLENDER_ITEMS_KEY, page(SLENDER_ITEMS_KEY, SLENDER_ITEMS_TITLE, meta -> {
                    addItem(meta, Items.slenderEye(), SLENDER_EYE_TEXT);
                    addItem(meta, Items.tracker(), TRACKER_TEXT);
                    addItem(meta, Items.possession(), POSSESSION_TEXT);
                }))));
    }

    /**
     * Returns the page a key leads to.
     *
     * @param key the key a button sent back
     * @return the page, or {@code null} for {@link #CLOSE_KEY} and for keys of other dialogs
     */
    @Nullable DialogTemplate page(Key key) {
        return this.pages.get(key);
    }

    /**
     * Gives the pages of one chapter their back and next buttons and stores them. The first page
     * goes back to the introduction, and the last one leads there instead of on.
     */
    private void addChapter(List<Page> chapter) {
        for (int i = 0; i < chapter.size(); i++) {
            Page page = chapter.get(i);
            Key back = i == 0 ? INTRO_KEY : chapter.get(i - 1).key();
            Consumer<ActionButton> forward = i == chapter.size() - 1
                    ? button(START_LABEL, INTRO_KEY)
                    : button(NEXT_LABEL, chapter.get(i + 1).key());
            this.pages.put(page.key(), page.dialog()
                    .columns(2)
                    .action(button(BACK_LABEL, back))
                    .action(forward)
                    .build());
        }
    }

    private static MultiActionDialog page(Key key, String title, Consumer<DialogMeta> body) {
        return DialogType.multiAction(Key.key("cygnus", "dialog/" + key.value()))
                .meta(meta -> {
                    meta.title(Messages.withMini(title))
                            .closeWithEscape(true)
                            .pause(false)
                            .afterAction(DialogAfterAction.NONE);
                    body.accept(meta);
                })
                .exitAction(button(CLOSE_LABEL, CLOSE_KEY));
    }

    private static Consumer<ActionButton> button(String label, Key target) {
        return button -> button.label(Messages.withMini(label)).action(new DialogAction.Custom(target, null));
    }

    /**
     * Formats one point of a rule page as its name, an arrow and the explanation.
     */
    private static String point(String name, String text) {
        return "<white><bold>" + name + "</bold> <gray>→</gray> " + text;
    }

    private static void addParagraphs(DialogMeta meta, List<String> paragraphs) {
        for (int i = 0; i < paragraphs.size(); i++) {
            if (i > 0) meta.emptyMessage();
            Component paragraph = Messages.withMini(paragraphs.get(i));
            meta.messageBody(body -> body.contents(paragraph));
        }
    }

    /**
     * Adds one item with its coloured name in front of its description, so the name a player reads
     * here is the one their hotbar shows.
     */
    private static void addItem(DialogMeta meta, ItemStack item, String text) {
        Component name = item.get(DataComponents.CUSTOM_NAME, Component.empty());
        Component description = Component.empty()
                .append(name)
                .append(Messages.withMini(" <gray>→</gray> <white>" + text));
        meta.itemBody(template -> template.item(item)
                .showToolTip(true)
                .description(body -> body.contents(description)));
    }

    /**
     * One page of a chapter before its buttons are added.
     *
     * @param key    the key that opens the page
     * @param dialog the page so far
     */
    private record Page(Key key, MultiActionDialog dialog) {
    }
}
