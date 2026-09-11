package net.onelitefeather.cygnus.common.page;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Associates custom page models (IDs 1 through 6) with atmospheric handwritten horror notes.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public enum PageNote {

    ALWAYS_WATCHES(1, "Always watches,\nno eyes"),
    DONT_LOOK(2, "Don't look\nor it takes you"),
    CANT_RUN(3, "Can't run"),
    LEAVE_ME_ALONE(4, "Leave me alone"),
    HELP_ME(5, "Help me"),
    NO_NO_NO(6, "No no no\nno...");

    private static final Pattern PAGE_MODEL_PATTERN = Pattern.compile("^(?:[a-z0-9_.-]+:)?page_(\\d+)$");

    private final int modelId;
    private final String text;
    private final Component component;
    private final Component tooltipComponent;

    PageNote(int modelId, String text) {
        this.modelId = modelId;
        this.text = text;
        this.component = Component.text(text, NamedTextColor.GRAY, TextDecoration.ITALIC);
        this.tooltipComponent = net.onelitefeather.cygnus.common.ui.TooltipBox.of(this.component);
    }

    /**
     * Returns the model ID associated with this note.
     *
     * @return the model ID
     */
    public int getModelId() {
        return modelId;
    }

    /**
     * Returns the raw note text.
     *
     * @return the note text
     */
    public String getText() {
        return text;
    }

    /**
     * Returns the formatted note as an Adventure {@link Component}.
     *
     * @return the note component
     */
     public Component getComponent() {
         return component;
     }

    /**
     * Returns the pre-built, ready-to-display tooltip component built at startup.
     *
     * @return the tooltip box component
     */
    public Component getTooltipComponent() {
        return tooltipComponent;
    }

    /**
     * Finds a {@link PageNote} by its numeric model ID.
     *
     * @param modelId the model ID (e.g. 1 to 6)
     * @return an {@link Optional} containing the note, or empty if unknown
     */
    public static Optional<PageNote> fromModelId(int modelId) {
        for (PageNote note : values()) {
            if (note.modelId == modelId) {
                return Optional.of(note);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the atmospheric note component for the specified custom model ID.
     *
     * @param modelId the model ID (e.g. 1 to 6)
     * @return an {@link Optional} containing the styled note component, or empty if unknown
     */
    @Contract(pure = true)
    public static Optional<Component> forCustomModel(int modelId) {
        return fromModelId(modelId).map(PageNote::getComponent);
    }

    /**
     * Returns the pre-built tooltip box component for the specified custom model ID.
     *
     * @param modelId the model ID (e.g. 1 to 6)
     * @return an {@link Optional} containing the pre-built tooltip component, or empty if unknown
     */
    @Contract(pure = true)
    public static Optional<Component> tooltipForCustomModel(int modelId) {
        return fromModelId(modelId).map(PageNote::getTooltipComponent);
    }

    /**
     * Extracts the custom page model ID from the given {@link ItemStack} if present in its
     * {@link DataComponents#ITEM_MODEL}.
     *
     * @param itemStack the item stack to extract the model ID from
     * @return an {@link OptionalInt} containing the model ID, or empty if not a page model item
     */
    public static OptionalInt extractModelId(@Nullable ItemStack itemStack) {
        if (itemStack == null || !itemStack.has(DataComponents.ITEM_MODEL)) {
            return OptionalInt.empty();
        }

        String model = itemStack.get(DataComponents.ITEM_MODEL);
        if (model == null) {
            return OptionalInt.empty();
        }

        Matcher matcher = PAGE_MODEL_PATTERN.matcher(model);
        if (matcher.matches()) {
            try {
                return OptionalInt.of(Integer.parseInt(matcher.group(1)));
            } catch (NumberFormatException ignored) {
                return OptionalInt.empty();
            }
        }
        return OptionalInt.empty();
    }

    /**
     * Resolves the atmospheric note component for the given {@link ItemStack} if it represents a custom page model.
     *
     * @param itemStack the item stack to resolve the note for
     * @return an {@link Optional} containing the note component, or empty if not a custom page model item
     */
    public static Optional<Component> forItem(@Nullable ItemStack itemStack) {
        OptionalInt modelId = extractModelId(itemStack);
        if (modelId.isEmpty()) {
            return Optional.empty();
        }
        return forCustomModel(modelId.getAsInt());
    }

    /**
     * Resolves the pre-built tooltip box component for the given {@link ItemStack} if it represents a custom page model.
     *
     * @param itemStack the item stack to resolve the tooltip for
     * @return an {@link Optional} containing the pre-built tooltip component, or empty if not a custom page model item
     */
    public static Optional<Component> tooltipForItem(@Nullable ItemStack itemStack) {
        OptionalInt modelId = extractModelId(itemStack);
        if (modelId.isEmpty()) {
            return Optional.empty();
        }
        return tooltipForCustomModel(modelId.getAsInt());
    }

    /**
     * Resolves the {@link PageNote} enum constant for the given {@link ItemStack} if it represents a custom page model.
     *
     * @param itemStack the item stack to resolve the note for
     * @return an {@link Optional} containing the {@link PageNote}, or empty if not a custom page model item
     */
    public static Optional<PageNote> fromItem(@Nullable ItemStack itemStack) {
        OptionalInt modelId = extractModelId(itemStack);
        if (modelId.isEmpty()) {
            return Optional.empty();
        }
        return fromModelId(modelId.getAsInt());
    }
}
