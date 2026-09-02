package net.onelitefeather.cygnus.setup.dialog;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.color.Color;
import net.minestom.server.dialog.DialogAction;
import net.minestom.server.dialog.DialogAfterAction;
import net.minestom.server.dialog.DialogInput;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.dimension.MapAtmosphere;
import net.onelitefeather.cygnus.common.dimension.StaticDimensionPreset;
import net.onelitefeather.cygnus.setup.util.DialogBase;
import net.onelitefeather.pica.dialog.DialogTemplate;
import net.onelitefeather.pica.dialog.type.DialogType;

/**
 * The dialog chain a map builder walks through to give a map its fog.
 *
 * <p>Pica only offers two-button confirm dialogs, so the flow is a chain rather than one dialog
 * with three buttons: pick a preset, adjust the values, look at the result, then save or adjust
 * again.</p>
 *
 * <p>{@link #SKY_LIGHT_SCALE} exists because the sky-light factor lives in a range roughly between
 * {@code 0.002} and {@code 0.08}, which no slider can resolve usefully. The slider works in whole
 * steps from 0 to 100 and the value is divided back down when it is read.</p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.3
 */
public final class AtmosphereDialogs extends DialogBase {

    /** Key of the preset selection dialog. */
    public static final Key PRESET_KEY = create("atmosphere_preset_dialog");

    /** Key of the value adjustment dialog. */
    public static final Key VALUES_KEY = create("atmosphere_values_dialog");

    /** Key of the dialog shown while a preview is running. */
    public static final Key CONFIRM_KEY = create("atmosphere_confirm_dialog");

    /** Key used when a builder wants to go back and adjust the values they are previewing. */
    public static final Key ADJUST_KEY = create("atmosphere_adjust_dialog");

    /** Input id of the preset dropdown. */
    public static final String PRESET_INPUT = "preset";

    /** Option id that keeps whatever values the map already carries. */
    public static final String KEEP_OPTION = "keep";

    /** Input ids of the value dialog. */
    public static final String FOG_START_INPUT = "fog_start";
    public static final String FOG_END_INPUT = "fog_end";
    public static final String SKY_FOG_END_INPUT = "sky_fog_end";
    public static final String SKY_LIGHT_INPUT = "sky_light";
    public static final String FOG_COLOR_INPUT = "fog_color";
    public static final String SKY_LIGHT_COLOR_INPUT = "sky_light_color";
    public static final String SKY_COLOR_INPUT = "sky_color";
    public static final String AMBIENT_LIGHT_COLOR_INPUT = "ambient_light_color";

    /** Factor between the whole-step slider and the fractional sky-light factor it stands for. */
    public static final float SKY_LIGHT_SCALE = 1000f;

    private static final int BUTTON_WIDTH = 101;
    private static final int HEX_LENGTH = 7;

    private AtmosphereDialogs() {
        // Nothing to do here
    }

    /**
     * Opens the first dialog of the chain, which offers the built-in presets as a starting point.
     *
     * @param player   who should see the dialog
     * @param hasValues whether the map already carries an atmosphere that can be kept
     */
    public static void openPresetDialog(Player player, boolean hasValues) {
        DialogTemplate template = DialogType.confirm(PRESET_KEY)
                .meta(meta -> {
                    meta.closeWithEscape(true);
                    meta.pause(false);
                    meta.afterAction(DialogAfterAction.CLOSE);
                    meta.title(Component.text("Fog"));
                    meta.emptyMessage();
                    meta.messageBody(body -> body.contents(
                            Component.text("Pick a starting point. You can adjust every value afterwards.")));
                    meta.option(PRESET_INPUT, option -> {
                        option.label(Component.text("Preset"));
                        option.with(BUTTON_WIDTH * 2);
                        option.option(new DialogInput.SingleOption.Option(
                                KEEP_OPTION,
                                Component.text("Keep current values", NamedTextColor.GRAY),
                                hasValues
                        ));
                        for (StaticDimensionPreset preset : StaticDimensionPreset.getValues()) {
                            option.option(new DialogInput.SingleOption.Option(
                                    preset.getKey(),
                                    Component.text(readable(preset.getKey())),
                                    !hasValues && preset == StaticDimensionPreset.DENSE_FOG
                            ));
                        }
                    });
                })
                .yesButton(button -> button.width(BUTTON_WIDTH).label(Component.text("Continue"))
                        .action(new DialogAction.DynamicCustom(PRESET_KEY, getEmptyPayload())))
                .noButton(button -> button.width(BUTTON_WIDTH).label(NO_COMPONENT))
                .build();
        template.open(player);
    }

    /**
     * Opens the value dialog, pre-filled with the given atmosphere.
     *
     * @param player     who should see the dialog
     * @param atmosphere the values the sliders and fields start at
     */
    public static void openValueDialog(Player player, MapAtmosphere atmosphere) {
        DialogTemplate template = DialogType.confirm(VALUES_KEY)
                .meta(meta -> {
                    meta.closeWithEscape(true);
                    meta.pause(false);
                    meta.afterAction(DialogAfterAction.CLOSE);
                    meta.title(Component.text("Fog"));
                    meta.emptyMessage();
                    meta.messageBody(body -> body.contents(
                            Component.text("Adjust the values, then preview them on the map.")));

                    meta.range(FOG_START_INPUT, range -> {
                        range.label(Component.text("Fog starts at"));
                        range.width(BUTTON_WIDTH * 2);
                        range.start(0f);
                        range.end(128f);
                        range.step(1f);
                        range.initial(atmosphere.fogStartDistance());
                        range.labelFormat("%s: %.0f blocks");
                    });
                    meta.range(FOG_END_INPUT, range -> {
                        range.label(Component.text("Fully fogged at"));
                        range.width(BUTTON_WIDTH * 2);
                        range.start(8f);
                        range.end(384f);
                        range.step(1f);
                        range.initial(atmosphere.fogEndDistance());
                        range.labelFormat("%s: %.0f blocks");
                    });
                    meta.range(SKY_FOG_END_INPUT, range -> {
                        range.label(Component.text("Horizon haze at"));
                        range.width(BUTTON_WIDTH * 2);
                        range.start(8f);
                        range.end(256f);
                        range.step(1f);
                        range.initial(atmosphere.skyFogEndDistance());
                        range.labelFormat("%s: %.0f blocks");
                    });
                    meta.range(SKY_LIGHT_INPUT, range -> {
                        range.label(Component.text("Sky light"));
                        range.width(BUTTON_WIDTH * 2);
                        range.start(0f);
                        range.end(100f);
                        range.step(1f);
                        range.initial(atmosphere.skyLightFactor() * SKY_LIGHT_SCALE);
                        range.labelFormat("%s: %.0f");
                    });

                    meta.text(FOG_COLOR_INPUT, text -> text.label(Component.text("Fog color"))
                            .maxLength(HEX_LENGTH).initial(asHex(atmosphere.fogColor())));
                    meta.text(SKY_LIGHT_COLOR_INPUT, text -> text.label(Component.text("Sky light color"))
                            .maxLength(HEX_LENGTH).initial(asHex(atmosphere.skyLightColor())));
                    meta.text(SKY_COLOR_INPUT, text -> text.label(Component.text("Sky color"))
                            .maxLength(HEX_LENGTH).initial(asHex(atmosphere.skyColor())));
                    meta.text(AMBIENT_LIGHT_COLOR_INPUT, text -> text.label(Component.text("Ambient light"))
                            .maxLength(HEX_LENGTH).initial(asHex(atmosphere.ambientLightColor())));
                })
                .yesButton(button -> button.width(BUTTON_WIDTH).label(Component.text("Preview"))
                        .action(new DialogAction.DynamicCustom(VALUES_KEY, getEmptyPayload())))
                .noButton(button -> button.width(BUTTON_WIDTH).label(Component.text("Cancel")))
                .build();
        template.open(player);
    }

    /**
     * Opens the dialog shown once a preview is running: keep the values, or go back and adjust.
     *
     * @param player who should see the dialog
     */
    public static void openConfirmDialog(Player player) {
        DialogTemplate template = DialogType.confirm(CONFIRM_KEY)
                .meta(meta -> {
                    meta.closeWithEscape(true);
                    meta.pause(false);
                    meta.afterAction(DialogAfterAction.CLOSE);
                    meta.title(Component.text("Fog"));
                    meta.emptyMessage();
                    meta.messageBody(body -> body.contents(
                            Component.text("This is how the map will look. Keep it?")));
                })
                .yesButton(button -> button.width(BUTTON_WIDTH).label(Component.text("Save"))
                        .action(new DialogAction.DynamicCustom(CONFIRM_KEY, getEmptyPayload())))
                .noButton(button -> button.width(BUTTON_WIDTH).label(Component.text("Adjust"))
                        .action(new DialogAction.DynamicCustom(ADJUST_KEY, getEmptyPayload())))
                .build();
        template.open(player);
    }

    /**
     * Renders a color the way {@code map.json} stores it, so what a builder sees in the field is
     * what ends up in the file.
     *
     * @param color the color to render
     * @return the color as {@code #RRGGBB}
     */
    public static String asHex(Color color) {
        return "#%02X%02X%02X".formatted(color.red(), color.green(), color.blue());
    }

    /**
     * Turns a preset key such as {@code dense_fog} into {@code Dense fog} for display.
     *
     * @param key the preset key
     * @return the key as a readable label
     */
    private static String readable(String key) {
        String spaced = key.replace('_', ' ');
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
}
