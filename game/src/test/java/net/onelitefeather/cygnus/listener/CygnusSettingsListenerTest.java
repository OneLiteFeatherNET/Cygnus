package net.onelitefeather.cygnus.listener;

import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.MainHand;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.listener.common.SettingsListener;
import net.minestom.server.message.ChatMessageType;
import net.minestom.server.network.packet.client.common.ClientSettingsPacket;
import net.minestom.server.network.player.ClientSettings;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers the crash from switching a {@link Player} to a non-avatar {@link EntityType} (e.g. the
 * Slender disguise, see {@link net.onelitefeather.cygnus.utils.Items#setSlenderEye}): Minestom's
 * built-in {@link SettingsListener} always casts the entity meta to {@code PlayerMeta}, which
 * blows up with a {@link ClassCastException} the next time the client resends its settings.
 */
@ExtendWith(MicrotusExtension.class)
class CygnusSettingsListenerTest {

    @Test
    void testVanillaListenerCrashesOnDisguise(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        player.switchEntityType(EntityType.ENDERMAN);

        ClientSettingsPacket packet = new ClientSettingsPacket(ClientSettings.DEFAULT);
        assertThrows(ClassCastException.class, () -> SettingsListener.listener(packet, player));

        env.destroyInstance(instance, true);
    }

    @Test
    void testNoCrashOnDisguise(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        player.switchEntityType(EntityType.ENDERMAN);

        ClientSettingsPacket packet = new ClientSettingsPacket(ClientSettings.DEFAULT);
        assertDoesNotThrow(() -> CygnusSettingsListener.listener(packet, player));

        env.destroyInstance(instance, true);
    }

    @Test
    void testAppliesSettingsForRegularPlayer(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        ClientSettings settings = new ClientSettings(
                Locale.GERMANY, (byte) 10,
                ChatMessageType.FULL, true,
                (byte) 0x7F, MainHand.LEFT,
                true, true,
                ClientSettings.ParticleSetting.MINIMAL
        );

        CygnusSettingsListener.listener(new ClientSettingsPacket(settings), player);

        assertEquals(MainHand.LEFT, player.getPlayerMeta().getMainHand());

        env.destroyInstance(instance, true);
    }
}
