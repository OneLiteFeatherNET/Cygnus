package net.onelitefeather.cygnus.resourcepack;

import net.kyori.adventure.resource.ResourcePackStatus;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerResourcePackStatusEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.common.ResourcePackPushPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.GameConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourcePackServiceTest extends CygnusPlayerTestBase {

    private static final URI PACK_URL = URI.create("https://example.com/pack.zip");
    private static final String PACK_HASH = "a".repeat(40);

    private static GameConfig config(@Nullable URI url, @Nullable String sha1) {
        return GameConfig.builder()
                .minPlayers(2)
                .maxPlayers(10)
                .lobbyTime(30)
                .gameTime(600)
                .resourcePackUrl(url)
                .resourcePackSha1(sha1)
                .build();
    }

    private ResourcePackService createService() {
        Optional<ResourcePackService> service = ResourcePackService.create(config(PACK_URL, PACK_HASH));
        assertTrue(service.isPresent());
        return service.get();
    }

    @Test
    void testCreateReturnsEmptyWhenNoUrlIsConfigured() {
        assertTrue(ResourcePackService.create(config(null, null)).isEmpty());
    }

    @Test
    void testCreateReturnsAServiceWhenOnlyTheUrlIsConfigured() {
        assertTrue(ResourcePackService.create(config(PACK_URL, null)).isPresent());
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

    @Test
    void testPackIdIsTheIdThePackWasPushedWith(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        ResourcePackService service = createService();
        Collector<ResourcePackPushPacket> pushes = connection.trackIncoming(ResourcePackPushPacket.class);

        service.sendTo(player);

        pushes.assertSingle(push -> assertEquals(service.packId(), push.id(),
                "the id the pack is taken back by has to be the id the client filed it under"));

        env.destroyInstance(instance, true);
    }

    @Test
    void testConfiguredHashIsPushedAsIs(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        ResourcePackService service = createService();
        Collector<ResourcePackPushPacket> pushes = connection.trackIncoming(ResourcePackPushPacket.class);

        service.sendTo(player);

        pushes.assertSingle(push -> assertEquals(PACK_HASH, push.hash()));

        env.destroyInstance(instance, true);
    }

    @Test
    void testMissingHashIsComputedFromThePack(@NotNull Env env, @TempDir Path tempDir)
            throws IOException, NoSuchAlgorithmException {
        Path pack = tempDir.resolve("pack.zip");
        Files.writeString(pack, "cygnus test pack");

        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        ResourcePackService service = ResourcePackService.create(config(pack.toUri(), null)).orElseThrow();
        Collector<ResourcePackPushPacket> pushes = connection.trackIncoming(ResourcePackPushPacket.class);

        service.sendTo(player);

        pushes.assertSingle(push -> assertEquals(sha1Of(pack), push.hash(),
                "a pack without a configured checksum has to be hashed by the server"));

        env.destroyInstance(instance, true);
    }

    @Test
    void testAnUnreachablePackIsNotPushed(@NotNull Env env, @TempDir Path tempDir) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        ResourcePackService service = ResourcePackService
                .create(config(tempDir.resolve("missing.zip").toUri(), null))
                .orElseThrow();

        service.sendTo(player);

        assertNull(player.getResourcePackFuture(),
                "a pack that cannot be hashed must not be pushed, and must not kick the player");
        assertTrue(player.isOnline());

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

    private static String sha1Of(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(file)));
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
