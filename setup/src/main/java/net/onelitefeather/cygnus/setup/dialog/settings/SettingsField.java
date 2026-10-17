package net.onelitefeather.cygnus.setup.dialog.settings;

import java.util.function.ToDoubleFunction;

/**
 * One slider of a settings dialog.
 *
 * @param key    the record value it stands for, and the key in the payload
 * @param label  what the slider is called in the dialog and in the slot lore
 * @param unit   what the value is counted in, shown after it, empty for a plain count
 * @param min    the lowest value the slider offers
 * @param max    the highest value the slider offers
 * @param step   how far one notch moves the slider
 * @param getter reads the value from the settings
 * @param <S>    the settings record
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.17.0
 */
public record SettingsField<S>(String key, String label, String unit, double min, double max, double step,
                               ToDoubleFunction<S> getter) {
}
