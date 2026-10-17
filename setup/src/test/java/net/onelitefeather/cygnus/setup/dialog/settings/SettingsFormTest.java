package net.onelitefeather.cygnus.setup.dialog.settings;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.FloatBinaryTag;
import net.onelitefeather.cygnus.common.map.settings.CreekSettings;
import net.onelitefeather.cygnus.common.map.settings.PageSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsFormTest {

    private static List<String> components(Class<? extends Record> type) {
        return Arrays.stream(type.getRecordComponents()).map(RecordComponent::getName).toList();
    }

    private static List<String> keys(SettingsForm<?> form) {
        return form.fields().stream().map(SettingsField::key).toList();
    }

    @Test
    @DisplayName("Every record value has exactly one field, in record order")
    void formsCoverEveryValue() {
        assertEquals(components(PageSettings.class), keys(SettingsForms.PAGES));
        assertEquals(components(CreekSettings.class), keys(SettingsForms.CREEK));
    }

    @Test
    @DisplayName("The defaults survive the way through the form")
    void defaultsRoundTrip() {
        assertEquals(PageSettings.DEFAULT, SettingsForms.PAGES.apply(SettingsForms.PAGES.values(PageSettings.DEFAULT)));
        assertEquals(CreekSettings.DEFAULT, SettingsForms.CREEK.apply(SettingsForms.CREEK.values(CreekSettings.DEFAULT)));
    }

    @Test
    @DisplayName("The sliders reach exactly as far as the record allows")
    void sliderBoundsMatchTheRecord() {
        SettingsField<CreekSettings> launch = SettingsForms.CREEK.fields().getLast();
        assertEquals("launchHeight", launch.key());
        assertEquals(0.0D, launch.min());
        assertEquals(CreekSettings.MAX_LAUNCH_HEIGHT, launch.max());
        SettingsField<PageSettings> respawn = SettingsForms.PAGES.fields().getLast();
        assertEquals(PageSettings.MIN_RESPAWN_DELAY_SECONDS, respawn.min());
        assertEquals(PageSettings.MAX_RESPAWN_DELAY_SECONDS, respawn.max());
    }

    @Test
    @DisplayName("The payload overrides, a missing key keeps the shown value")
    void payloadIsReadWithFallback() {
        Map<String, Double> shown = SettingsForms.CREEK.values(CreekSettings.DEFAULT);
        CompoundBinaryTag payload = CompoundBinaryTag.builder()
                .put("sightRange", FloatBinaryTag.floatBinaryTag(30f))
                .build();

        Map<String, Double> read = SettingsForms.CREEK.read(payload, shown);

        assertEquals(30.0D, read.get("sightRange"));
        assertEquals(15.0D, read.get("personalSpace"));
    }

    @Test
    @DisplayName("A contradiction throws, and the entered values are still there to reopen the dialog with")
    void contradictionThrowsAndKeepsTheInput() {
        Map<String, Double> shown = SettingsForms.CREEK.values(CreekSettings.DEFAULT);
        CompoundBinaryTag payload = CompoundBinaryTag.builder()
                .put("personalSpace", FloatBinaryTag.floatBinaryTag(30f))
                .build();
        Map<String, Double> entered = SettingsForms.CREEK.read(payload, shown);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> SettingsForms.CREEK.apply(entered));
        assertEquals(30.0D, entered.get("personalSpace"));
        assertTrue(exception.getMessage().contains("personalSpace"), exception.getMessage());
    }

    @Test
    @DisplayName("A rejected contradiction is told with the slider labels instead of the record keys")
    void contradictionNamesTheSliderLabels() {
        Map<String, Double> entered = SettingsForms.CREEK.values(CreekSettings.DEFAULT);
        entered.put("personalSpace", 30.0D);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> SettingsForms.CREEK.apply(entered));

        assertEquals("'Keeps away by' (30) must be below 'Stalks from at least' (20)",
                SettingsForms.CREEK.describe(exception.getMessage()));
    }
}
