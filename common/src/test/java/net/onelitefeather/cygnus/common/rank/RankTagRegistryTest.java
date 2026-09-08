package net.onelitefeather.cygnus.common.rank;

import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankTagRegistryTest {

    private RankTagRegistry registry;

    @BeforeEach
    void setUp() {
        this.registry = new RankTagRegistry();
        this.registry.registerDefaults();
    }

    @Test
    void testStandardDefaults() {
        RankTagRegistry standard = RankTagRegistry.standard();
        assertNotNull(standard);

        assertTrue(standard.findById("administrator").isPresent());
        assertTrue(standard.findById("assistent").isPresent());
        assertTrue(standard.findById("mod").isPresent());
        assertTrue(standard.findById("content").isPresent());
        assertTrue(standard.findById("media").isPresent());
        assertTrue(standard.findById("lite").isPresent());
        assertTrue(standard.findById("player").isPresent());
    }

    @Test
    void testResolveByGroupStandardAndAliases() {
        assertEquals(Optional.of(RankTag.ADMINISTRATOR), registry.resolveByGroup("administrator"));
        assertEquals(Optional.of(RankTag.ADMINISTRATOR), registry.resolveByGroup("admin"));
        assertEquals(Optional.of(RankTag.ADMINISTRATOR), registry.resolveByGroup("ADMIN"));
        assertEquals(Optional.of(RankTag.ADMINISTRATOR), registry.resolveByGroup("owner"));

        assertEquals(Optional.of(RankTag.ASSISTANT), registry.resolveByGroup("assistent"));
        assertEquals(Optional.of(RankTag.ASSISTANT), registry.resolveByGroup("assistant"));
        assertEquals(Optional.of(RankTag.ASSISTANT), registry.resolveByGroup("sr_mod"));

        assertEquals(Optional.of(RankTag.MOD), registry.resolveByGroup("mod"));
        assertEquals(Optional.of(RankTag.MOD), registry.resolveByGroup("moderator"));

        assertEquals(Optional.of(RankTag.CONTENT), registry.resolveByGroup("content"));
        assertEquals(Optional.of(RankTag.CONTENT), registry.resolveByGroup("developer"));
        assertEquals(Optional.of(RankTag.CONTENT), registry.resolveByGroup("builder"));

        assertEquals(Optional.of(RankTag.MEDIA), registry.resolveByGroup("media"));
        assertEquals(Optional.of(RankTag.MEDIA), registry.resolveByGroup("creator"));
        assertEquals(Optional.of(RankTag.MEDIA), registry.resolveByGroup("twitch"));

        assertEquals(Optional.of(RankTag.LITE), registry.resolveByGroup("lite"));
        assertEquals(Optional.of(RankTag.LITE), registry.resolveByGroup("vip"));
        assertEquals(Optional.of(RankTag.LITE), registry.resolveByGroup("premium"));

        assertEquals(Optional.of(RankTag.PLAYER), registry.resolveByGroup("player"));
        assertEquals(Optional.of(RankTag.PLAYER), registry.resolveByGroup("default"));

        assertEquals(Optional.empty(), registry.resolveByGroup("unknown_group"));
        assertEquals(Optional.empty(), registry.resolveByGroup(null));
        assertEquals(Optional.empty(), registry.resolveByGroup("   "));
    }

    @Test
    void testRegisterCustomTag() {
        RankTag tester = new RankTag("tester", "󱆛", Key.key("olf", "rank_tags"), 30);
        registry.register(tester, "tester", "qa_lead");

        assertEquals(Optional.of(tester), registry.findById("tester"));
        assertEquals(Optional.of(tester), registry.resolveByGroup("tester"));
        assertEquals(Optional.of(tester), registry.resolveByGroup("qa_lead"));
    }

    @Test
    void testFallbackWhenLuckPermsAbsent() {
        UUID testUuid = UUID.randomUUID();
        RankTag primary = registry.resolvePrimary(testUuid);
        assertSame(RankTag.PLAYER, primary);

        List<RankTag> available = registry.resolveAvailable(testUuid);
        assertEquals(1, available.size());
        assertSame(RankTag.PLAYER, available.getFirst());

        assertSame(RankTag.PLAYER, registry.resolvePrimary((UUID) null));
    }

    @Test
    void testCustomFallbackTag() {
        RankTag customFallback = new RankTag("guest", "󱆖", -1);
        registry.setFallbackTag(customFallback);

        assertSame(customFallback, registry.getFallbackTag());
        assertSame(customFallback, registry.resolvePrimary((UUID) null));
    }

    @Test
    void testInvalidRegistrations() {
        assertThrows(NullPointerException.class, () -> registry.register(null, "foo"));
        assertThrows(NullPointerException.class, () -> registry.setFallbackTag(null));
    }
}
