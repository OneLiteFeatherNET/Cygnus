package net.onelitefeather.cygnus.common.page;

import org.jetbrains.annotations.Nullable;

/**
 * Data entry representing a page note read from {@code page_notes.json}.
 *
 * @param id   the numeric custom model ID (1..6)
 * @param name the logical name or enum identifier
 * @param text the atmospheric note text (can contain newlines)
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public record PageNoteEntry(int id, @Nullable String name, String text) {
}
