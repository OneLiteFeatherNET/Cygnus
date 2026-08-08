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

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Sends a mandatory ResourcePack to players and kicks them if the client declines it or reports
 * a failure. Inactive unless {@link ResourcePackProperties#resolve()} finds both the
 * {@code resourcepack.url} and {@code resourcepack.hash} system properties set.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ResourcePackService {

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

    private final ResourcePackInfo packInfo;

    private ResourcePackService(ResourcePackProperties properties) {
        this.packInfo = ResourcePackInfo.resourcePackInfo(UUID.randomUUID(), properties.url(), properties.hash());
    }

    /**
     * Creates a new {@link ResourcePackService} from the resolved {@link ResourcePackProperties}.
     *
     * @return the service, or empty if the feature is disabled
     */
    public static Optional<ResourcePackService> create() {
        return ResourcePackProperties.resolve().map(ResourcePackService::new);
    }

    /**
     * Sends the configured, mandatory ResourcePack request to the given player.
     *
     * @param player the player to send the request to
     */
    public void sendTo(Player player) {
        ResourcePackRequest request = ResourcePackRequest.resourcePackRequest()
                .packs(packInfo)
                .required(true)
                .prompt(PROMPT)
                .build();
        player.sendResourcePacks(request);
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
}
