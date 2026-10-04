package net.onelitefeather.cygnus.creek.tab;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.creek.dread.CreekWitness;
import net.onelitefeather.cygnus.team.RoleIcon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HuntedTabWitnessTest extends CygnusPlayerTestBase {

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static Player join(Env env, Instance instance) {
        return env.createConnection().connect(instance, new Pos(0, 40, 0));
    }

    private static HuntedTabWitness witness(Player... players) {
        Map<UUID, Player> byId = new HashMap<>();
        for (Player player : players) byId.put(player.getUuid(), player);
        return new HuntedTabWitness(CreekWitness.NONE, byId::get);
    }

    private static Component survivorName(Player player) {
        return RoleIcon.SURVIVOR.prefix(Component.text(player.getUsername(), NamedTextColor.GREEN));
    }

    @Test
    @DisplayName("A hunted survivor's tab name carries the marker and the name turns red")
    void huntedSurvivorIsHighlighted(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = join(env, instance);
        survivor.setDisplayName(survivorName(survivor));

        witness(survivor).hunted(survivor.getUuid());

        Component shown = survivor.getDisplayName();
        assertTrue(plain(shown).contains(HuntedTabWitness.MARKER), "the marker is missing: " + plain(shown));
        assertTrue(plain(shown).contains(survivor.getUsername()), "the name is missing: " + plain(shown));
        assertNotEquals(survivorName(survivor), shown, "the display name was not changed");
    }

    @Test
    @DisplayName("The role icon keeps its own font and color under the highlight")
    void roleIconSurvivesTheHighlight(Env env) {
        Component marked = HuntedTabWitness.highlight(RoleIcon.SURVIVOR.prefix(Component.text("Alex", NamedTextColor.GREEN)));

        assertTrue(contains(marked, RoleIcon.SURVIVOR.glyph()), "the role glyph must be untouched");
        assertTrue(containsRedText(marked, "Alex"), "the name must be red");
    }

    @Test
    @DisplayName("A spectator's strike-through stays when a highlighted name is composed")
    void strikeThroughIsKept(Env env) {
        Component struck = Component.text("Alex", NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH);

        Component marked = HuntedTabWitness.highlight(struck);

        assertTrue(hasDecoration(marked, "Alex"), "the strike-through must survive");
    }

    @Test
    @DisplayName("When the hunt ends the exact previous display name is back")
    void huntEndRestoresTheName(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = join(env, instance);
        Component before = survivorName(survivor);
        survivor.setDisplayName(before);
        HuntedTabWitness witness = witness(survivor);
        witness.hunted(survivor.getUuid());

        witness.huntEnded(survivor.getUuid());

        assertEquals(before, survivor.getDisplayName());
    }

    @Test
    @DisplayName("A survivor without a display name gets none again after the hunt")
    void noDisplayNameStaysNone(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = join(env, instance);
        survivor.setDisplayName(null);
        HuntedTabWitness witness = witness(survivor);

        witness.hunted(survivor.getUuid());
        assertTrue(plain(survivor.getDisplayName()).contains(HuntedTabWitness.MARKER));
        witness.huntEnded(survivor.getUuid());

        assertNull(survivor.getDisplayName());
    }

    @Test
    @DisplayName("A spectator name set during the hunt is not overwritten when the hunt ends")
    void spectatorNameWins(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = join(env, instance);
        survivor.setDisplayName(survivorName(survivor));
        HuntedTabWitness witness = witness(survivor);
        witness.hunted(survivor.getUuid());
        Component spectator = RoleIcon.SPECTATOR.prefix(
                Component.text(survivor.getUsername(), NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH));
        survivor.setDisplayName(spectator);

        witness.huntEnded(survivor.getUuid());

        assertEquals(spectator, survivor.getDisplayName());
    }

    @Test
    @DisplayName("Ending a hunt that never started changes nothing")
    void endWithoutStartIsHarmless(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = join(env, instance);
        Component before = survivorName(survivor);
        survivor.setDisplayName(before);

        witness(survivor).huntEnded(survivor.getUuid());

        assertEquals(before, survivor.getDisplayName());
    }

    @Test
    @DisplayName("Two hunters on one survivor keep the mark until the last hunt ends")
    void twoHuntsOnOneSurvivor(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = join(env, instance);
        Component before = survivorName(survivor);
        survivor.setDisplayName(before);
        HuntedTabWitness witness = witness(survivor);
        witness.hunted(survivor.getUuid());
        witness.hunted(survivor.getUuid());

        witness.huntEnded(survivor.getUuid());
        assertTrue(plain(survivor.getDisplayName()).contains(HuntedTabWitness.MARKER), "still hunted by the second creek");
        witness.huntEnded(survivor.getUuid());

        assertEquals(before, survivor.getDisplayName());
    }

    @Test
    @DisplayName("Two hunted survivors are marked and restored independently")
    void twoTargets(Env env) {
        Instance instance = env.createFlatInstance();
        Player first = join(env, instance);
        Player second = join(env, instance);
        Component firstBefore = survivorName(first);
        Component secondBefore = survivorName(second);
        first.setDisplayName(firstBefore);
        second.setDisplayName(secondBefore);
        HuntedTabWitness witness = witness(first, second);
        witness.hunted(first.getUuid());
        witness.hunted(second.getUuid());

        witness.huntEnded(first.getUuid());

        assertEquals(firstBefore, first.getDisplayName());
        assertTrue(plain(second.getDisplayName()).contains(HuntedTabWitness.MARKER), "the second is still hunted");
        witness.huntEnded(second.getUuid());
        assertEquals(secondBefore, second.getDisplayName());
    }

    @Test
    @DisplayName("A hunted survivor who left is skipped without error")
    void goneSurvivorIsSkipped(Env env) {
        HuntedTabWitness witness = witness();
        UUID gone = UUID.randomUUID();

        witness.hunted(gone);
        witness.huntEnded(gone);
    }

    @Test
    @DisplayName("Everything else the creek reports still reaches the wrapped witness")
    void delegatesTheRest(Env env) {
        UUID id = UUID.randomUUID();
        StringBuilder log = new StringBuilder();
        CreekWitness recording = new CreekWitness() {
            @Override
            public void sighted(UUID survivor) {
                log.append("sighted;");
            }

            @Override
            public void caught(UUID survivor) {
                log.append("caught;");
            }

            @Override
            public void stalked(UUID survivor) {
                log.append("stalked;");
            }

            @Override
            public void selected(UUID survivor) {
                log.append("selected;");
            }

            @Override
            public void hunted(UUID survivor) {
                log.append("hunted;");
            }

            @Override
            public void huntEnded(UUID survivor) {
                log.append("ended;");
            }
        };
        HuntedTabWitness witness = new HuntedTabWitness(recording, _ -> null);

        witness.sighted(id);
        witness.caught(id);
        witness.stalked(id);
        witness.selected(id);
        witness.hunted(id);
        witness.huntEnded(id);

        assertEquals("sighted;caught;stalked;selected;hunted;ended;", log.toString());
    }

    private static boolean contains(Component root, Component wanted) {
        if (root.equals(wanted)) return true;
        return root.children().stream().anyMatch(child -> contains(child, wanted));
    }

    private static boolean containsRedText(Component root, String text) {
        if (root instanceof net.kyori.adventure.text.TextComponent t && t.content().equals(text)) {
            return NamedTextColor.RED.equals(t.color());
        }
        return root.children().stream().anyMatch(child -> containsRedText(child, text));
    }

    private static boolean hasDecoration(Component root, String text) {
        if (root instanceof net.kyori.adventure.text.TextComponent t && t.content().equals(text)) {
            return t.decoration(TextDecoration.STRIKETHROUGH) == TextDecoration.State.TRUE;
        }
        return root.children().stream().anyMatch(child -> hasDecoration(child, text));
    }
}
