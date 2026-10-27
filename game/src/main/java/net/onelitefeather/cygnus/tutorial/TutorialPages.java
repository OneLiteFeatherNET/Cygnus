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
 * The five pages of the lobby tutorial: the introduction, and for each role one page on how it
 * plays and one on its items.
 *
 * <p>Every button reports a key of its own, and every key but {@link #CLOSE_KEY} names the page it
 * leads to, so turning a page needs nothing but the key the client sends back. The pages are built
 * once and shared by every player.</p>
 *
 * <p>All pages stay open after a click. The next page then replaces the current one directly;
 * closing first would show the world for a moment in between. Escape closes the tutorial, since
 * nothing on it has to be read.</p>
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
    /** Opens the survivors' page on how they play. */
    static final Key SURVIVOR_RULES_KEY = Key.key("cygnus", "tutorial/survivor/rules");
    /** Opens the survivors' item page. */
    static final Key SURVIVOR_ITEMS_KEY = Key.key("cygnus", "tutorial/survivor/items");
    /** Opens Slenderman's page on how he plays. */
    static final Key SLENDER_RULES_KEY = Key.key("cygnus", "tutorial/slender/rules");
    /** Opens Slenderman's item page. */
    static final Key SLENDER_ITEMS_KEY = Key.key("cygnus", "tutorial/slender/items");
    /** Closes the tutorial. It has no page of its own. */
    static final Key CLOSE_KEY = Key.key("cygnus", "tutorial/close");

    private static final String INTRO_TITLE = "<gold><bold>How to play";
    private static final List<String> INTRO_BODY = List.of(
            "<white>One of you is <bold>Slenderman</bold>. Everyone else is a <bold>survivor</bold>.",
            "<white>The survivors search the map for pages. Find enough of them before the time runs out and "
                    + "you win. Slenderman wins if nobody is left to find them.",
            "<white>You are not alone with him: the <bold>creek</bold>, a second figure, walks the map as well, "
                    + "and it answers to nobody.",
            "<white>Pick a role to learn more.");

    private static final String SURVIVOR_RULES_TITLE = "<green><bold>Survivor — How to survive";
    private static final List<String> SURVIVOR_RULES_BODY = List.of(
            "<white><bold>Pages.</bold> Pages are spread across the map, but they do not stay forever. A page "
                    + "that is about to vanish starts to chime, faster the less time it has left.",
            "<white><bold>Fear.</bold> Your level shows your sanity: 100 is calm, 0 is terrified. Pages, the "
                    + "creek and the death of a teammate scare you. Fear wears off again, but less and less as "
                    + "the round goes on, and the creek goes for the most scared.",
            "<white><bold>Sprint.</bold> The green bar is your breath. Sprinting drains it, and it refills once "
                    + "you stop. Run it empty and you cannot sprint again until it has filled up a good part of "
                    + "the way.",
            "<white><bold>Slenderman.</bold> Most of the time you cannot see him. When he shows himself he is "
                    + "close, and he hits whoever is near him. Your screen tears apart while he stands in your "
                    + "view. When he comes close, adrenaline lets you run faster for a moment.",
            "<white><bold>The creek.</bold> If it catches you, it drags you to where Slenderman started.",
            "<white><bold>Stay together.</bold> Not everything that looks like a teammate still is one.");

    private static final String SURVIVOR_ITEMS_TITLE = "<green><bold>Survivor — Your items";

    private static final String SLENDER_RULES_TITLE = "<dark_red><bold>Slenderman — How to hunt";
    private static final List<String> SLENDER_RULES_BODY = List.of(
            "<white><bold>Goal.</bold> Stop the survivors before they find enough pages.",
            "<white><bold>Hidden and shown.</bold> While hidden, the survivors cannot see you. Show yourself to "
                    + "hit whoever is near you, but while you are shown you are slower, half blind and cannot "
                    + "sprint. When your bar runs out you vanish again and need time to recover before you can "
                    + "show yourself once more.",
            "<white><bold>Traces.</bold> While hidden you now and then leave a footprint that only survivors "
                    + "can see.",
            "<white><bold>Pages.</bold> Every page they take scrambles your own screen a bit more.",
            "<white><bold>The creek.</bold> It hunts the survivors on its own. Use it: look through its eyes to "
                    + "find them.");

    private static final String SLENDER_ITEMS_TITLE = "<dark_red><bold>Slenderman — Your items";

    private static final String PAGE_REVEAL_TEXT = "Shows the pages around you on your locator bar for a few "
            + "seconds. It only points roughly to them, so the last steps are up to you. Does not work while "
            + "sprinting.";
    private static final String PAINKILLERS_TEXT = "Win back some health over a few seconds. Only once per round, "
            + "so save them for when you need them. Fear makes them work slower.";
    private static final String SLENDER_EYE_TEXT = "Hide yourself, or show yourself to the survivors.";
    private static final String TRACKER_TEXT = "Reveals where survivors walked around you a little while ago. "
            + "Only you can see the tracks. Locked for a while at the start of a round.";
    private static final String POSSESSION_TEXT = "While hidden, look through the eyes of the creek. Survivors "
            + "close to it show on your locator bar. Your body stays behind, frozen and visible. Sneak to return.";

    private final Map<Key, DialogTemplate> pages;

    /**
     * Builds all five pages.
     */
    TutorialPages() {
        this.pages = new HashMap<>();
        this.pages.put(INTRO_KEY, page(INTRO_KEY, INTRO_TITLE, meta -> addParagraphs(meta, INTRO_BODY))
                .columns(3)
                .action(button("<green>Survivor", SURVIVOR_RULES_KEY))
                .action(button("<dark_red>Slenderman", SLENDER_RULES_KEY))
                .action(button("<gray>Close", CLOSE_KEY))
                .build());
        this.pages.put(SURVIVOR_RULES_KEY, page(SURVIVOR_RULES_KEY, SURVIVOR_RULES_TITLE,
                meta -> addParagraphs(meta, SURVIVOR_RULES_BODY))
                .columns(2)
                .action(button("<white>Your items", SURVIVOR_ITEMS_KEY))
                .action(button("<gray>Back", INTRO_KEY))
                .build());
        this.pages.put(SURVIVOR_ITEMS_KEY, page(SURVIVOR_ITEMS_KEY, SURVIVOR_ITEMS_TITLE, meta -> {
                    addItem(meta, Items.pageReveal(), PAGE_REVEAL_TEXT);
                    addItem(meta, Items.painkillers(), PAINKILLERS_TEXT);
                })
                .columns(2)
                .action(button("<white>How to survive", SURVIVOR_RULES_KEY))
                .action(button("<gray>Back", INTRO_KEY))
                .build());
        this.pages.put(SLENDER_RULES_KEY, page(SLENDER_RULES_KEY, SLENDER_RULES_TITLE,
                meta -> addParagraphs(meta, SLENDER_RULES_BODY))
                .columns(2)
                .action(button("<white>Your items", SLENDER_ITEMS_KEY))
                .action(button("<gray>Back", INTRO_KEY))
                .build());
        this.pages.put(SLENDER_ITEMS_KEY, page(SLENDER_ITEMS_KEY, SLENDER_ITEMS_TITLE, meta -> {
                    addItem(meta, Items.slenderEye(), SLENDER_EYE_TEXT);
                    addItem(meta, Items.tracker(), TRACKER_TEXT);
                    addItem(meta, Items.possession(), POSSESSION_TEXT);
                })
                .columns(2)
                .action(button("<white>How to hunt", SLENDER_RULES_KEY))
                .action(button("<gray>Back", INTRO_KEY))
                .build());
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

    private static MultiActionDialog page(Key key, String title, Consumer<DialogMeta> body) {
        return DialogType.multiAction(Key.key("cygnus", "dialog/" + key.value()))
                .meta(meta -> {
                    meta.title(Messages.withMini(title))
                            .closeWithEscape(true)
                            .pause(false)
                            .afterAction(DialogAfterAction.NONE);
                    body.accept(meta);
                });
    }

    private static Consumer<ActionButton> button(String label, Key target) {
        return button -> button.label(Messages.withMini(label)).action(new DialogAction.Custom(target, null));
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
                .append(Messages.withMini("<white>: " + text));
        meta.itemBody(template -> template.item(item)
                .showToolTip(true)
                .description(body -> body.contents(description)));
    }
}
