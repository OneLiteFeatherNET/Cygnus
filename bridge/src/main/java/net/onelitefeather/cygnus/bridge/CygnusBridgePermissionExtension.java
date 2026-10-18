package net.onelitefeather.cygnus.bridge;

import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.modules.bridge.impl.platform.minestom.MinestomPermissionChecker;
import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.util.TriState;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.extensions.Extension;
import net.onelitefeather.cygnus.common.event.RoundInGameEvent;
import net.onelitefeather.minestom.extensions.processor.ExtensionInfo;

/**
 * Minestom extension that teaches the CloudNet bridge how Cygnus resolves permissions.
 * <p>
 * The bridge ships a default checker that only inspects {@code player.getPermissionLevel()}, which
 * is always {@code 0} on a LuckPerms-managed server — maintenance bypass and task-level
 * {@code requiredPermission} checks would therefore reject every player, staff included. This
 * extension registers a checker that reads Adventure's {@link PermissionChecker#POINTER} instead,
 * the same pointer LuckPerms and our {@code /stop} command read, and marks it the registry default.
 * <p>
 * {@link MinestomPermissionChecker} only exists inside the CloudNet bridge's extension classloader,
 * so this glue cannot live in the application. The {@code CloudNet_Bridge} dependency declared below
 * makes this extension load after the bridge and share its classloader hierarchy. Minestom and
 * Adventure come from the application classloader above, so the pointer read here is the very one
 * the player carries.
 * <p>
 * The same extension also reports the round as in game: on {@link RoundInGameEvent} it switches the
 * service to {@code INGAME} so CloudNet starts a fresh service of the task. A jar carries only one
 * extension entry point, so this lives here and shares the {@code CloudNet_Bridge} dependency.
 * <p>
 * {@link ExtensionInfo} generates {@code extension.json} at compile time; the version is supplied by
 * the build through {@code -Aminestom.extension.version}.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.6.7
 **/
@ExtensionInfo(
        name = "CygnusCloudNetPermissions",
        authors = "OneLiteFeather",
        dependencies = "CloudNet_Bridge"
)
public final class CygnusBridgePermissionExtension extends Extension {

    private final EventNode<Event> inGameNode = EventNode.all("cygnus-bridge-ingame");

    @Override
    public void initialize() {
        MinestomPermissionChecker checker = (player, permission) ->
                player.getOrDefault(PermissionChecker.POINTER, PermissionChecker.always(TriState.FALSE))
                        .test(permission);
        ServiceRegistry.registry()
                .registerProvider(MinestomPermissionChecker.class, "cygnus-luckperms", checker)
                .markAsDefaultService();

        inGameNode.addListener(RoundInGameEvent.class, new InGameListener(new CloudNetServiceStateSwitch()));
        MinecraftServer.getGlobalEventHandler().addChild(inGameNode);
    }

    @Override
    public void terminate() {
        MinecraftServer.getGlobalEventHandler().removeChild(inGameNode);
    }
}
