package net.onelitefeather.cygnus.common.page;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MicrotusExtension.class)
class PageNoteTest {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    @Test
    void testMappingsForKnownModelIds(Env ignored) {
        String[] expectedNotes = {
                "Always watches, no eyes",
                "Don't look or it takes you",
                "Can't run",
                "Leave me alone",
                "Help me",
                "No no no no..."
        };

        for (int id = 1; id <= 6; id++) {
            Optional<Component> noteOpt = PageNote.forCustomModel(id);
            assertTrue(noteOpt.isPresent(), "Model ID " + id + " should have a note");

            Component note = noteOpt.get();
            assertEquals(TextDecoration.State.TRUE, note.decoration(TextDecoration.ITALIC),
                    "Note for model ID " + id + " should be italic");
            assertEquals(expectedNotes[id - 1], PLAIN.serialize(note),
                    "Note text for model ID " + id + " should match expected");
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 7, 8, 100})
    void testUnknownModelIdsReturnEmpty(int unknownId) {
        Optional<Component> noteOpt = PageNote.forCustomModel(unknownId);
        assertTrue(noteOpt.isEmpty(), "Unknown model ID " + unknownId + " should return empty");

        Optional<PageNote> pageNoteOpt = PageNote.fromModelId(unknownId);
        assertTrue(pageNoteOpt.isEmpty(), "Unknown model ID " + unknownId + " should return empty enum");
    }

    @Test
    void testExtractModelIdFromItemStack(Env ignored) {
        ItemStack itemWithModel = ItemStack.builder(Material.PAPER)
                .set(DataComponents.ITEM_MODEL, Key.key("cygnus", "page_4").asString())
                .build();

        OptionalInt modelId = PageNote.extractModelId(itemWithModel);
        assertTrue(modelId.isPresent());
        assertEquals(4, modelId.getAsInt());

        Optional<Component> note = PageNote.forItem(itemWithModel);
        assertTrue(note.isPresent());
        assertEquals("Leave me alone", PLAIN.serialize(note.get()));
    }

    @Test
    void testExtractModelIdFromInvalidItems(Env ignored) {
        assertFalse(PageNote.extractModelId(null).isPresent());
        assertFalse(PageNote.forItem(null).isPresent());

        ItemStack plainPaper = ItemStack.of(Material.PAPER);
        assertFalse(PageNote.extractModelId(plainPaper).isPresent());
        assertFalse(PageNote.forItem(plainPaper).isPresent());

        ItemStack nonPageModel = ItemStack.builder(Material.PAPER)
                .set(DataComponents.ITEM_MODEL, "minecraft:paper")
                .build();
        assertFalse(PageNote.extractModelId(nonPageModel).isPresent());
        assertFalse(PageNote.forItem(nonPageModel).isPresent());
    }

    @Test
    void testPageCreatorItemCompatibility(Env ignored) {
        PageCreator creator = new PageCreator() {};
        for (int i = 0; i < 20; i++) {
            ItemStack pageItem = creator.createPageItem(1);
            OptionalInt modelId = PageNote.extractModelId(pageItem);
            assertTrue(modelId.isPresent());
            assertTrue(modelId.getAsInt() >= 1 && modelId.getAsInt() <= 6);

            Optional<Component> note = PageNote.forItem(pageItem);
            assertTrue(note.isPresent());
            assertFalse(PLAIN.serialize(note.get()).isBlank());
        }
    }
}
