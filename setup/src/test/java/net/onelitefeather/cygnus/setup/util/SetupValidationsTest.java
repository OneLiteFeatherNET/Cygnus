package net.onelitefeather.cygnus.setup.util;

import de.icevizion.aves.map.BaseMap;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MicrotusExtension.class)
class SetupValidationsTest {

    @Test
    void testArgCondition(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        assertTrue(SetupValidations.argCondition(true, player, Component.empty()));
        env.destroyInstance(instance, true);
    }

    @Test
    void testMapCondition(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        assertTrue(SetupValidations.mapCondition(null, player));
        assertFalse(SetupValidations.mapCondition(new BaseMap(), player));
        env.destroyInstance(instance, true);
    }
}