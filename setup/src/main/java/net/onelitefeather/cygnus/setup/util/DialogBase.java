package net.onelitefeather.cygnus.setup.util;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Contract;

public abstract class DialogBase {

    protected static final Component NO_COMPONENT = Component.text("No");

    /**
     * Creates a new instance of a {@link Key}.
     *
     * @param value the value of the key
     * @return the created instance
     */
    protected static Key create(String value) {
        return Key.key("cygnus", value);
    }

    /**
     * Creates a new {@link CompoundBinaryTag} with no data for the dialog
     *
     * @return the created tag
     */
    @Contract(pure = true)
    protected static CompoundBinaryTag getEmptyPayload() {
        return CompoundBinaryTag.builder().build();
    }
}
