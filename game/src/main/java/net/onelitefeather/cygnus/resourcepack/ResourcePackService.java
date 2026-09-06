package net.onelitefeather.cygnus.resourcepack;

import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.resource.ResourcePackStatus;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerResourcePackStatusEvent;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.config.GameConfig;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Sends a mandatory ResourcePack to players and kicks them if the client declines it or reports
 * a failure. Inactive unless {@link GameConfig#resourcePackUrl()} is configured.
 *
 * @author theEvilReaper
 * @version 1.2.0
 * @since 1.0.0
 */
public final class ResourcePackService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcePackService.class);

    // DISCARDED is included because Minestom's own Player#onResourcePackStatus kicks on it anyway
    // once required(true) is set (DISCARDED is not an 'intermediate' ResourcePackStatus) — listing it
    // here means our KICK_MESSAGE is shown instead of Minestom's generic fallback message.
    private static final Set<ResourcePackStatus> KICK_STATUSES = EnumSet.of(
            ResourcePackStatus.DECLINED,
            ResourcePackStatus.FAILED_DOWNLOAD,
            ResourcePackStatus.INVALID_URL,
            ResourcePackStatus.FAILED_RELOAD,
            ResourcePackStatus.DISCARDED
    );

    private static final Component PROMPT =
            Messages.withMini("<gray>This server requires you to accept a custom <yellow>ResourcePack</yellow> to play.");
    private static final Component KICK_MESSAGE =
            Messages.withMini("<red>You must accept the ResourcePack to play on this server!");

    // Downloading the pack to hash it happens on the configuration thread of the joining player,
    // so it must not be able to hang that join forever.
    private static final long HASH_TIMEOUT_SECONDS = 30;

    private final UUID packId;
    private final URI url;
    private @Nullable CompletableFuture<ResourcePackInfo> packInfo;

    private ResourcePackService(URI url, @Nullable String hash) {
        this.packId = UUID.randomUUID();
        this.url = url;
        // Without a configured checksum there is nothing to build the info from yet; it is computed
        // from the pack itself when the first player needs it (see #packInfoFuture()).
        this.packInfo = hash == null
                ? null
                : CompletableFuture.completedFuture(ResourcePackInfo.resourcePackInfo(this.packId, url, hash));
    }

    /**
     * Creates a new {@link ResourcePackService} from the game configuration.
     *
     * @param config the configuration holding the ResourcePack URL and, optionally, its checksum
     * @return the service, or empty if the feature is disabled
     */
    public static Optional<ResourcePackService> create(GameConfig config) {
        URI url = config.resourcePackUrl();
        if (url == null) {
            return Optional.empty();
        }
        return Optional.of(new ResourcePackService(url, config.resourcePackSha1()));
    }

    /**
     * Sends the configured, mandatory ResourcePack request to the given player.
     * <p>
     * If the checksum has to be computed first, this waits for that computation - the caller is
     * the joining player's configuration thread, which is meant to be blocked on. A pack that
     * cannot be hashed is not pushed at all; the player joins without it rather than being kicked
     * for something the server got wrong.
     * </p>
     *
     * @param player the player to send the request to
     */
    public void sendTo(Player player) {
        ResourcePackInfo info = resolvePackInfo();
        if (info == null) {
            return;
        }
        ResourcePackRequest request = ResourcePackRequest.resourcePackRequest()
                .packs(info)
                .required(true)
                .prompt(PROMPT)
                .build();
        player.sendResourcePacks(request);
    }

    /**
     * Returns the id the pushed pack is keyed by on the client.
     *
     * <p>A client keeps the packs a server pushed in a stack keyed by this id, and it only drops one
     * when a {@code ResourcePackPopPacket} names that id. Behind a proxy the connection outlives this
     * service - the player is moved to another backend, not disconnected - so nothing clears the
     * stack on its own and the pack has to be popped explicitly. See
     * {@link net.onelitefeather.cygnus.player.CygnusPlayer#kick(net.kyori.adventure.text.Component)},
     * which is where that happens.</p>
     *
     * @return the id of the pack this service pushes
     * @since 2.11.0
     */
    public UUID packId() {
        return packId;
    }

    /**
     * Registers the status listener that kicks players who decline or fail to load the pack.
     *
     * @param node the event node to register the listener on
     */
    public void registerListener(EventNode<Event> node) {
        node.addListener(PlayerResourcePackStatusEvent.class, this::handleStatus);
    }

    void handleStatus(PlayerResourcePackStatusEvent event) {
        if (KICK_STATUSES.contains(event.getStatus())) {
            event.getPlayer().kick(KICK_MESSAGE);
        }
    }

    /**
     * Waits for the pack info to become available.
     *
     * @return the info to push, or {@code null} if the checksum could not be computed
     */
    private @Nullable ResourcePackInfo resolvePackInfo() {
        CompletableFuture<ResourcePackInfo> future = packInfoFuture();
        try {
            return future.get(HASH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Interrupted while computing the SHA-1 of '{}'. Not pushing the ResourcePack", url);
            return null;
        } catch (ExecutionException | TimeoutException exception) {
            LOGGER.warn("Failed to compute the SHA-1 of '{}'. Not pushing the ResourcePack", url, exception);
            // A failed download is worth retrying on the next join. A timeout is not: that
            // computation is still running and will complete the very future kept here.
            forgetFailedComputation(future);
            return null;
        }
    }

    /**
     * Returns the pack info, starting its computation on first use if no checksum was configured.
     *
     * @return the future carrying the pack info
     */
    private synchronized CompletableFuture<ResourcePackInfo> packInfoFuture() {
        if (packInfo == null) {
            LOGGER.warn("No ResourcePack checksum configured - computing the SHA-1 of '{}' once. " +
                    "This is meant for test setups; a production setup should state the checksum", url);
            packInfo = ResourcePackInfo.resourcePackInfo()
                    .id(packId)
                    .uri(url)
                    .computeHashAndBuild();
        }
        return packInfo;
    }

    /**
     * Drops a computation that failed, so the next join starts a fresh one.
     *
     * @param failed the future that was waited on
     */
    private synchronized void forgetFailedComputation(CompletableFuture<ResourcePackInfo> failed) {
        if (packInfo == failed && failed.isCompletedExceptionally()) {
            packInfo = null;
        }
    }
}
