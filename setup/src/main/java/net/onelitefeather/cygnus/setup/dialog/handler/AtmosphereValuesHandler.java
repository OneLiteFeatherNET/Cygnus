package net.onelitefeather.cygnus.setup.dialog.handler;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.DoubleBinaryTag;
import net.kyori.adventure.nbt.FloatBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.minestom.server.color.Color;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.onelitefeather.cygnus.common.dimension.MapAtmosphere;
import net.onelitefeather.cygnus.common.map.GameMapBuilder;
import net.onelitefeather.cygnus.setup.atmosphere.AtmospherePreviewService;
import net.onelitefeather.cygnus.setup.data.GameData;
import net.onelitefeather.cygnus.setup.dialog.AtmosphereDialogs;
import net.onelitefeather.guira.SetupDataService;
import org.jetbrains.annotations.Nullable;

/**
 * Reads the adjusted values out of the dialog, stores them on the map builder and starts a preview
 * of them.
 *
 * <p>The values are written to the builder before the preview runs, so the builder's own dialog
 * chain and the inventory show the same numbers the preview is rendering. Cancelling the preview
 * clears them again.</p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.3
 */
public final class AtmosphereValuesHandler implements DialogHandler {

    private final SetupDataService dataService;
    private final AtmospherePreviewService previewService;

    /**
     * Creates a new handler.
     *
     * @param dataService    the service holding the setup of each builder
     * @param previewService the service that renders a preview of the values
     */
    public AtmosphereValuesHandler(SetupDataService dataService, AtmospherePreviewService previewService) {
        this.dataService = dataService;
        this.previewService = previewService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handle(PlayerCustomClickEvent event, CompoundBinaryTag payload) {
        Player player = event.getPlayer();

        this.dataService.get(player.getUuid()).ifPresent(data -> {
            if (!(data instanceof GameData gameData)) return;

            GameMapBuilder builder = (GameMapBuilder) gameData.getMapBuilder();
            MapAtmosphere previous = builder.getAtmosphere();
            MapAtmosphere atmosphere = read(payload, previous);

            builder.setAtmosphere(atmosphere);
            gameData.triggerUpdate(GameData.InventoryTarget.GENERAL);

            this.previewService.preview(player, atmosphere, gameData.getMapEntry().getDirectoryRoot());
            AtmosphereDialogs.openConfirmDialog(player);
        });
    }

    /**
     * Builds an atmosphere from the dialog payload, falling back to the previous values for any
     * field the client did not send back.
     *
     * @param payload  the dialog payload
     * @param previous the atmosphere the map carried before, if any
     * @return the atmosphere the builder just configured
     */
    private static MapAtmosphere read(CompoundBinaryTag payload, @Nullable MapAtmosphere previous) {
        return new MapAtmosphere(
                color(payload.get(AtmosphereDialogs.FOG_COLOR_INPUT), previous == null ? null : previous.fogColor()),
                color(payload.get(AtmosphereDialogs.SKY_LIGHT_COLOR_INPUT), previous == null ? null : previous.skyLightColor()),
                color(payload.get(AtmosphereDialogs.SKY_COLOR_INPUT), previous == null ? null : previous.skyColor()),
                number(payload.get(AtmosphereDialogs.SKY_LIGHT_INPUT), 8f) / AtmosphereDialogs.SKY_LIGHT_SCALE,
                number(payload.get(AtmosphereDialogs.FOG_START_INPUT), 0f),
                number(payload.get(AtmosphereDialogs.FOG_END_INPUT), 48f),
                number(payload.get(AtmosphereDialogs.SKY_FOG_END_INPUT), 32f)
        );
    }

    /**
     * Reads a numeric dialog input, whatever numeric tag the client happened to send it as.
     *
     * @param tag          the raw value
     * @param defaultValue the value used when the tag is absent or not a number
     * @return the value as a float
     */
    private static float number(@Nullable BinaryTag tag, float defaultValue) {
        return switch (tag) {
            case FloatBinaryTag value -> value.value();
            case DoubleBinaryTag value -> (float) value.value();
            case IntBinaryTag value -> value.value();
            case StringBinaryTag value -> parse(value.value(), defaultValue);
            case null, default -> defaultValue;
        };
    }

    /**
     * Parses a numeric input that arrived as text.
     *
     * @param raw          the text to parse
     * @param defaultValue the value used when the text is not a number
     * @return the parsed value, or the default
     */
    private static float parse(String raw, float defaultValue) {
        try {
            return Float.parseFloat(raw.trim());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    /**
     * Reads a hex color field, keeping the previous color when the builder typed something that is
     * not a color. Rejecting the whole dialog over a typo in one field would throw away the other
     * six values with it.
     *
     * @param tag      the raw value
     * @param previous the color the map carried before, if any
     * @return the parsed color, the previous one, or black
     */
    private static Color color(@Nullable BinaryTag tag, @Nullable Color previous) {
        Color fallback = previous != null ? previous : Color.fromRGBLike(Color.BLACK);
        if (!(tag instanceof StringBinaryTag string)) return fallback;

        String raw = string.value().trim();
        String hex = raw.startsWith("#") ? raw.substring(1) : raw;
        if (hex.length() != 6) return fallback;

        try {
            return new Color(Integer.parseInt(hex, 16));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
