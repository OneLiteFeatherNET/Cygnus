package net.onelitefeather.cygnus.resourcepack;

import net.kyori.adventure.resource.ResourcePackStatus;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerResourcePackStatusEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourcePackServiceTest extends CygnusPlayerTestBase {

    @AfterEach
    void clearSystemProperties() {
        System.clearProperty("resourcepack.url");
        System.clearProperty("resourcepack.hash");
    }

    private ResourcePackService createService() {
        System.setProperty("resourcepack.url", "https://example.com/pack.zip");
        System.setProperty("resourcepack.hash", "a".repeat(40));
        Optional<ResourcePackService> service = ResourcePackService.create();
        assertTrue(service.isPresent());
        return service.get();
    }

    @Test
    void testCreateReturnsEmptyWhenPropertiesAreAbsent() {
        assertTrue(ResourcePackService.create().isEmpty());
    }

    @Test
    void testSendToRegistersAPendingResourcePack(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        ResourcePackService service = createService();

        assertNull(player.getResourcePackFuture());

        service.sendTo(player);

        assertNotNull(player.getResourcePackFuture());
        assertFalse(player.getResourcePackFuture().isDone());

        env.destroyInstance(instance, true);
    }

    @ParameterizedTest
    @EnumSource(value = ResourcePackStatus.class, names = {"DECLINED", "FAILED_DOWNLOAD", "INVALID_URL", "FAILED_RELOAD", "DISCARDED"})
    void testHandleStatusKicksOnTriggerStatuses(ResourcePackStatus status, @NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        ResourcePackService service = createService();

        service.handleStatus(new PlayerResourcePackStatusEvent(player, UUID.randomUUID(), status));

        assertFalse(player.isOnline(), "Expected a kick for status " + status);

        env.destroyInstance(instance, true);
    }

    @ParameterizedTest
    @EnumSource(value = ResourcePackStatus.class, names = {"DECLINED", "FAILED_DOWNLOAD", "INVALID_URL", "FAILED_RELOAD", "DISCARDED"}, mode = EnumSource.Mode.EXCLUDE)
    void testHandleStatusDoesNotKickOnOtherStatuses(ResourcePackStatus status, @NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        ResourcePackService service = createService();

        service.handleStatus(new PlayerResourcePackStatusEvent(player, UUID.randomUUID(), status));

        assertTrue(player.isOnline(), "Did not expect a kick for status " + status);

        env.destroyInstance(instance, true);
    }
}
