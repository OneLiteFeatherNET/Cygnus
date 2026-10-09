package net.onelitefeather.cygnus.common.config;

import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.function.Supplier;

/**
 * A part of the loaded {@code config.properties}: every key below one prefix, such as {@code creek.}.
 * <p>
 * A missing key gives the fallback. So does an unreadable value, with a warning: a value the
 * operator cannot have meant is not worth taking the service down for. Values out of range are
 * left to the config records, which reject them.
 * </p>
 *
 * @param properties the loaded properties
 * @param prefix     the prefix in front of every key of this section, empty for the root
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.16.0
 */
public record ConfigSection(Properties properties, String prefix) {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigSection.class);

    /**
     * Returns the whole file, for keys without a prefix.
     *
     * @param properties the loaded properties
     * @return the section with the empty prefix
     */
    public static ConfigSection root(Properties properties) {
        return new ConfigSection(properties, "");
    }

    /**
     * Returns the part below another prefix, appended to this one.
     *
     * @param prefix the prefix to append, for example {@code creek.}
     * @return the nested section
     */
    public ConfigSection section(String prefix) {
        return new ConfigSection(this.properties, this.prefix + prefix);
    }

    /**
     * Returns the full key in the file, as the warnings name it.
     *
     * @param key the key inside this section
     * @return the key with the prefix in front
     */
    public String key(String key) {
        return this.prefix + key;
    }

    /**
     * Reads a trimmed value.
     *
     * @param key the key inside this section
     * @return the value, or {@code null} if the key is absent or holds nothing but whitespace
     */
    public @Nullable String getString(String key) {
        String value = this.properties.getProperty(this.key(key));
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Reads a whole number.
     *
     * @param key      the key inside this section
     * @param fallback the value to use when the key is absent or unreadable
     * @return the parsed value
     */
    public int getInt(String key, int fallback) {
        String value = this.getString(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException _) {
            LOGGER.warn("Failed to parse integer config value for key '{}': '{}'. Falling back to default: {}", this.key(key), value, fallback);
            return fallback;
        }
    }

    /**
     * Reads a decimal.
     *
     * @param key      the key inside this section
     * @param fallback the value to use when the key is absent or unreadable
     * @return the parsed value
     */
    public float getFloat(String key, float fallback) {
        String value = this.getString(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException _) {
            LOGGER.warn("Failed to parse decimal config value for key '{}': '{}'. Falling back to default: {}", this.key(key), value, fallback);
            return fallback;
        }
    }

    /**
     * Reads a decimal with double precision. The creek's values are compared with each other, and
     * float rounding would turn 0.6 into 0.6000000238.
     *
     * @param key      the key inside this section
     * @param fallback the value to use when the key is absent or unreadable
     * @return the parsed value
     */
    public double getDouble(String key, double fallback) {
        String value = this.getString(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException _) {
            LOGGER.warn("Failed to parse decimal config value for key '{}': '{}'. Falling back to default: {}", this.key(key), value, fallback);
            return fallback;
        }
    }

    /**
     * Reads a flag. Anything other than {@code true} or {@code false} is not a decision the operator
     * made on purpose, so it falls back instead of silently counting as {@code false} the way
     * {@link Boolean#parseBoolean(String)} would.
     *
     * @param key      the key inside this section
     * @param fallback the value to use when the key is absent or unreadable
     * @return the parsed flag
     */
    public boolean getBoolean(String key, boolean fallback) {
        String value = this.getString(key);
        if (value == null) {
            return fallback;
        }
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        LOGGER.warn("Failed to parse boolean config value for key '{}': '{}'. Falling back to default: {}", this.key(key), value, fallback);
        return fallback;
    }

    /**
     * Reads a sound key. Only the key syntax is checked here. Whether the key names a sound the
     * client knows is not something the config can answer.
     *
     * @param key      the key inside this section
     * @param fallback the sound to use when the key is absent or malformed
     * @return the parsed sound key
     */
    public Key getSound(String key, Key fallback) {
        String value = this.getString(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Key.key(value);
        } catch (InvalidKeyException exception) {
            LOGGER.warn("'{}' is not a valid sound key: '{}'. Falling back to default: {}", this.key(key), value, fallback, exception);
            return fallback;
        }
    }

    /**
     * Builds a group of values and falls back to its defaults if the group rejects them. A value
     * the operator got wrong is not worth taking the service down for.
     *
     * @param group    the group's name for the warning, for example {@code creek stalk}
     * @param read     builds the group from this section
     * @param fallback the group's defaults
     * @param <T>      the group's type
     * @return the group, or the fallback if its values break a rule
     */
    public <T> T orDefault(String group, Supplier<T> read, T fallback) {
        try {
            return read.get();
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Invalid values for {}: {}. Falling back to its defaults", group, exception.getMessage());
            return fallback;
        }
    }
}
