package net.onelitefeather.cygnus.common.dimension;

import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.particle.Particle;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;
import net.minestom.server.world.attribute.AmbientParticle;
import net.minestom.server.world.attribute.EnvironmentAttribute;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Small factory class to create custom {@link RegistryKey<DimensionType>} instances from custom dimension presets.
 *
 * @author Joltra
 * @version 1.0.0
 * @since 2.6.6
 */
public final class DimensionFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(DimensionFactory.class);

    private DimensionFactory() {
        throw new UnsupportedOperationException();
    }

    public static void registerAll() {
        for (StaticDimensionPreset value : StaticDimensionPreset.getValues()) {
            RegistryKey<DimensionType> dimensionType = create(value);
            LOGGER.info("Registered dimension preset: {}", dimensionType.key());
        }
    }

    /**
     * Creates a new dimension type based on the given preset.
     *
     * @param preset which should be created
     * @return the created dimension type
     */
    @Contract(value = "_ -> new", pure = true)
    public static RegistryKey<DimensionType> create(DimensionPreset preset) {
        DimensionType type = DimensionType.builder()
                .setAttribute(EnvironmentAttribute.FOG_START_DISTANCE, preset.fogStartDistance())
                .setAttribute(EnvironmentAttribute.FOG_END_DISTANCE, preset.fogEndDistance())
                .setAttribute(EnvironmentAttribute.SKY_FOG_END_DISTANCE, preset.skyFogEndDistance())
                .setAttribute(EnvironmentAttribute.FOG_COLOR, preset.fogColor())
                .setAttribute(EnvironmentAttribute.SKY_COLOR, preset.skyColor())
                .setAttribute(EnvironmentAttribute.SKY_LIGHT_COLOR, preset.skyLightColor())
                .setAttribute(EnvironmentAttribute.SKY_LIGHT_FACTOR, preset.skyLightFactor())
                .setAttribute(EnvironmentAttribute.AMBIENT_PARTICLES, List.of(
                        new AmbientParticle(Particle.SOUL, 0.01f),
                        new AmbientParticle(Particle.SOUL_FIRE_FLAME, 0.001f)
                ))
                .setAttribute(EnvironmentAttribute.SUN_ANGLE, 180f)
                .setAttribute(EnvironmentAttribute.MOON_ANGLE, 180f)
                .defaultClock(DimensionType.OVERWORLD.asValue().defaultClock())
                .build();
        return MinecraftServer.getDimensionTypeRegistry().register(Key.key("cygnus", preset.getKey()), type);
    }
}