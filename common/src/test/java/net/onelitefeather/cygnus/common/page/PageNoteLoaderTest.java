package net.onelitefeather.cygnus.common.page;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PageNoteLoaderTest {

    @Test
    void testLoadBundledNotes() {
        Map<Integer, String> notes = PageNoteLoader.loadBundledNotes();
        assertNotNull(notes);
        assertFalse(notes.isEmpty());
        assertEquals(6, notes.size());
        assertEquals("Always watches,\nno eyes", notes.get(1));
        assertEquals("Don't look\nor it takes you", notes.get(2));
        assertEquals("Can't run", notes.get(3));
        assertEquals("Leave me alone", notes.get(4));
        assertEquals("Help me", notes.get(5));
        assertEquals("No no no\nno...", notes.get(6));
    }

    @Test
    void testLoadExternalNotes(@TempDir Path tempDir) throws IOException {
        Path jsonFile = tempDir.resolve("custom_notes.json");
        String json = """
                [
                  {
                    "id": 1,
                    "name": "CUSTOM_1",
                    "text": "Custom note 1"
                  },
                  {
                    "id": 2,
                    "name": "CUSTOM_2",
                    "text": "Custom line 1\\nCustom line 2"
                  }
                ]
                """;
        Files.writeString(jsonFile, json);

        Map<Integer, String> notes = PageNoteLoader.loadNotes(jsonFile);
        assertNotNull(notes);
        assertEquals(2, notes.size());
        assertEquals("Custom note 1", notes.get(1));
        assertEquals("Custom line 1\nCustom line 2", notes.get(2));
    }

    @Test
    void testLoadNonExistentFileFallsBackToBundled(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("missing.json");
        Map<Integer, String> notes = PageNoteLoader.loadNotes(missing);
        assertNotNull(notes);
        assertEquals(6, notes.size());
    }
}
