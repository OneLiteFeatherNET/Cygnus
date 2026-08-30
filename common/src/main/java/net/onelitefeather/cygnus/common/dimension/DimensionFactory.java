package net.onelitefeather.cygnus.common.dimension;

import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;
import net.minestom.server.world.attribute.EnvironmentAttribute;
import org.jetbrains.annotations.Contract;

/**
 * Small factory class to create custom {@link RegistryKey<DimensionType>} instances from custom
 * dimension presets.
 * <p>
 * Registry data only reaches a client during its configuration phase, so every dimension a player
 * may end up in has to be registered before that player logs in - or the player has to be sent
 * back through a configuration phase, which is what the setup module's atmosphere preview does.
 * </p>
 *
 * @author Joltra
 * @version 2.0.0
 * @since 2.6.6
 */
public final class DimensionFactory {

    private DimensionFactory() {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates and registers a new dimension type under the given registry key.
     *
     * @param registryKey the key the dimension is registered under
     * @param atmosphere  the values the dimension should carry
     * @return the key of the registered dimension type
     */
    @Contract(value = "_, _ -> new", pure = true)
    public static RegistryKey<DimensionType> create(Key registryKey, DimensionAtmosphere atmosphere) {
        DimensionType type = DimensionType.builder()
                .setAttribute(EnvironmentAttribute.FOG_START_DISTANCE, atmosphere.fogStartDistance())
                .setAttribute(EnvironmentAttribute.FOG_END_DISTANCE, atmosphere.fogEndDistance())
                .setAttribute(EnvironmentAttribute.SKY_FOG_END_DISTANCE, atmosphere.skyFogEndDistance())
                .setAttribute(EnvironmentAttribute.FOG_COLOR, atmosphere.fogColor())
                .setAttribute(EnvironmentAttribute.SKY_COLOR, atmosphere.skyColor())
                .setAttribute(EnvironmentAttribute.SKY_LIGHT_COLOR, atmosphere.skyLightColor())
                .setAttribute(EnvironmentAttribute.SKY_LIGHT_FACTOR, atmosphere.skyLightFactor())
                .setAttribute(EnvironmentAttribute.SUN_ANGLE, 180f)
                .setAttribute(EnvironmentAttribute.MOON_ANGLE, 180f)
                .defaultClock(DimensionType.OVERWORLD.asValue().defaultClock())
                .build();
        return MinecraftServer.getDimensionTypeRegistry().register(registryKey, type);
    }

    /**
     * Creates and registers a new dimension type from the given preset, using the preset's own key
     * inside the {@code cygnus} namespace.
     *
     * @param preset which should be created
     * @return the key of the registered dimension type
     */
    @Contract(value = "_ -> new", pure = true)
    public static RegistryKey<DimensionType> create(DimensionPreset preset) {
        return create(Key.key("cygnus", preset.getKey()), preset);
    }
}
