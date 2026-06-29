package net.onelitefeather.cygnus.common.dimension;

import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.Clock;
import net.minestom.server.instance.Instance;
import net.minestom.server.particle.Particle;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;
import net.minestom.server.world.attribute.AmbientParticle;
import net.minestom.server.world.attribute.EnvironmentAttribute;
import net.minestom.server.world.clock.WorldClock;

import java.util.List;
import java.util.Random;

public final class DimensionFactory {

    private DimensionFactory() {
        throw new UnsupportedOperationException();
    }

    private static String s;

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
                .defaultClock(WorldClock.OVERWORLD)
                .build();
        s += "s";
        return MinecraftServer.getDimensionTypeRegistry().register(Key.key("test", s), type);
    }
}