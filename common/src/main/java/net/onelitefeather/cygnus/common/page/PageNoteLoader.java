package net.onelitefeather.cygnus.common.page;

import com.google.gson.reflect.TypeToken;
import net.onelitefeather.cygnus.common.util.GsonHelper;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads atmospheric page notes from a JSON configuration file (external file or bundled classpath resource).
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class PageNoteLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(PageNoteLoader.class);
    private static final String DEFAULT_RESOURCE = "/page_notes.json";
    private static final Type LIST_TYPE = new TypeToken<List<PageNoteEntry>>() {}.getType();

    private PageNoteLoader() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Loads page note entries from an external file path, falling back to the bundled resource
     * if the file does not exist or fails to parse.
     *
     * @param externalPath path to the external JSON file, or {@code null} to load bundled default
     * @return an unmodifiable map of model ID to note text
     */
    public static Map<Integer, String> loadNotes(@Nullable Path externalPath) {
        if (externalPath != null && Files.exists(externalPath)) {
            try (InputStream stream = Files.newInputStream(externalPath)) {
                Map<Integer, String> notes = parseStream(stream);
                if (!notes.isEmpty()) {
                    LOGGER.info("Loaded {} page notes from external file: {}", notes.size(), externalPath);
                    return notes;
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to load page notes from {}, falling back to bundled default", externalPath, e);
            }
        }

        return loadBundledNotes();
    }

    /**
     * Loads page note entries from the bundled classpath resource {@code /page_notes.json}.
     *
     * @return an unmodifiable map of model ID to note text
     */
    public static Map<Integer, String> loadBundledNotes() {
        try (InputStream stream = PageNoteLoader.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (stream == null) {
                LOGGER.error("Bundled resource '{}' not found on classpath", DEFAULT_RESOURCE);
                return Collections.emptyMap();
            }
            Map<Integer, String> notes = parseStream(stream);
            LOGGER.info("Loaded {} page notes from bundled resource", notes.size());
            return notes;
        } catch (Exception e) {
            LOGGER.error("Failed to parse bundled page notes resource", e);
            return Collections.emptyMap();
        }
    }

    private static Map<Integer, String> parseStream(InputStream stream) {
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            List<PageNoteEntry> list = GsonHelper.GSON.fromJson(reader, LIST_TYPE);
            if (list == null || list.isEmpty()) {
                return Collections.emptyMap();
            }

            Map<Integer, String> map = new HashMap<>();
            for (PageNoteEntry entry : list) {
                if (entry != null && entry.text() != null) {
                    map.put(entry.id(), entry.text());
                }
            }
            return Collections.unmodifiableMap(map);
        } catch (Exception e) {
            LOGGER.warn("Failed to read page notes from JSON stream", e);
            return Collections.emptyMap();
        }
    }
}
