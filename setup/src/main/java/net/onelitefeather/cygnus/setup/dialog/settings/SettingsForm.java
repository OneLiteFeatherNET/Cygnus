package net.onelitefeather.cygnus.setup.dialog.settings;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.DoubleBinaryTag;
import net.kyori.adventure.nbt.FloatBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.onelitefeather.cygnus.common.map.GameMapBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Describes a settings record so one dialog, one slot and one handler serve every kind of map settings.
 *
 * @param title    the dialog title and the name in messages
 * @param saveKey  the dialog key of the save button
 * @param resetKey the dialog key of the reset button
 * @param fields   one field per record value, in record order
 * @param defaults what the reset button sets
 * @param create   builds the record from the values, keyed by field key; throws if they do not fit
 * @param get      reads the settings from the map builder
 * @param set      writes the settings to the map builder
 * @param <S>      the settings record
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.17.0
 */
public record SettingsForm<S>(
        String title,
        Key saveKey,
        Key resetKey,
        List<SettingsField<S>> fields,
        S defaults,
        Function<Map<String, Double>, S> create,
        Function<GameMapBuilder, S> get,
        BiConsumer<GameMapBuilder, S> set
) {

    /**
     * Reads every value of the settings, in field order.
     *
     * @param settings the settings to read
     * @return the values, keyed by field key
     */
    public Map<String, Double> values(S settings) {
        Map<String, Double> values = new LinkedHashMap<>();
        for (SettingsField<S> field : this.fields) {
            values.put(field.key(), field.getter().applyAsDouble(settings));
        }
        return values;
    }

    /**
     * Reads what the player set in the dialog. A slider the payload leaves out keeps the value shown.
     *
     * @param payload  the dialog payload
     * @param fallback the values the dialog was opened with
     * @return the entered values, keyed by field key
     */
    public Map<String, Double> read(CompoundBinaryTag payload, Map<String, Double> fallback) {
        Map<String, Double> values = new LinkedHashMap<>();
        for (SettingsField<S> field : this.fields) {
            Double value = number(payload.get(field.key()));
            values.put(field.key(), value != null ? value : fallback.get(field.key()));
        }
        return values;
    }

    /**
     * Builds the settings from the values.
     *
     * @param values the values, keyed by field key
     * @return the settings
     * @throws IllegalArgumentException if a value is out of range or two values contradict each other
     */
    public S apply(Map<String, Double> values) {
        return this.create.apply(values);
    }

    /**
     * Swaps the record keys in a reason for the slider labels, so it names what the player saw.
     *
     * @param reason why the values were rejected, naming record keys
     * @return the reason naming slider labels
     */
    public String describe(String reason) {
        String described = reason;
        for (SettingsField<S> field : this.fields) {
            described = described.replace(field.key(), "'" + field.label() + "'");
        }
        return described;
    }

    private static @Nullable Double number(@Nullable BinaryTag tag) {
        return switch (tag) {
            case FloatBinaryTag value -> (double) value.value();
            case DoubleBinaryTag value -> value.value();
            case IntBinaryTag value -> (double) value.value();
            case null, default -> null;
        };
    }
}
