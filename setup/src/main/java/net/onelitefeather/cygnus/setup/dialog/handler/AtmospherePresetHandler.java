package net.onelitefeather.cygnus.setup.dialog.handler;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.onelitefeather.cygnus.common.dimension.MapAtmosphere;
import net.onelitefeather.cygnus.common.dimension.StaticDimensionPreset;
import net.onelitefeather.cygnus.common.map.GameMapBuilder;
import net.onelitefeather.cygnus.setup.data.GameData;
import net.onelitefeather.cygnus.setup.dialog.AtmosphereDialogs;
import net.onelitefeather.guira.SetupDataService;
import org.jetbrains.annotations.Nullable;

/**
 * Turns the preset a builder picked into a set of starting values and moves the chain on to the
 * value dialog.
 *
 * <p>The preset is only ever a starting point: what gets stored is the resolved set of values, so a
 * map keeps looking the way it was set up even if the preset it started from is changed later.</p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.3
 */
public final class AtmospherePresetHandler implements DialogHandler {

    /** Preset a map falls back to when it has no values yet and the builder keeps the default. */
    private static final StaticDimensionPreset FALLBACK = StaticDimensionPreset.DENSE_FOG;

    private final SetupDataService dataService;

    /**
     * Creates a new handler.
     *
     * @param dataService the service holding the setup of each builder
     */
    public AtmospherePresetHandler(SetupDataService dataService) {
        this.dataService = dataService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handle(PlayerCustomClickEvent event, CompoundBinaryTag payload) {
        this.dataService.get(event.getPlayer().getUuid()).ifPresent(data -> {
            if (!(data instanceof GameData gameData)) return;

            GameMapBuilder builder = (GameMapBuilder) gameData.getMapBuilder();
            MapAtmosphere current = builder.getAtmosphere();
            MapAtmosphere starting = resolve(payload.get(AtmosphereDialogs.PRESET_INPUT), current);

            AtmosphereDialogs.openValueDialog(event.getPlayer(), starting);
        });
    }

    /**
     * Resolves the selected option into the values the next dialog starts from.
     *
     * @param selection the raw dialog payload of the preset dropdown
     * @param current   the atmosphere the map carries today, if any
     * @return the values to pre-fill the value dialog with
     */
    private static MapAtmosphere resolve(@Nullable BinaryTag selection, @Nullable MapAtmosphere current) {
        String option = selection instanceof StringBinaryTag string ? string.value() : AtmosphereDialogs.KEEP_OPTION;

        if (AtmosphereDialogs.KEEP_OPTION.equals(option)) {
            return current != null ? current : MapAtmosphere.from(FALLBACK);
        }

        for (StaticDimensionPreset preset : StaticDimensionPreset.getValues()) {
            if (preset.getKey().equals(option)) {
                return MapAtmosphere.from(preset);
            }
        }
        return current != null ? current : MapAtmosphere.from(FALLBACK);
    }
}
