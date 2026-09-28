package net.onelitefeather.cygnus.command;

import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.util.TriState;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.creek.debug.CreekDebug;

/**
 * Turns the creek's debug line in the action bar on or off. Meant for playtests.
 *
 * <p>Requires {@value #PERMISSION}, like {@code GlitchCommand}: the line shows where the creek is
 * and who it is after, which normal players must not know. Only players can use it, because the
 * console has no action bar.</p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class CreekCommand extends Command {

    private static final String PERMISSION = "cygnus.command.creek";

    /**
     * Creates the command.
     *
     * @param debug the debug line to toggle for the sender
     */
    public CreekCommand(CreekDebug debug) {
        super("creek");
        setCondition((sender, commandString) -> sender instanceof Player);

        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) return;

            boolean watching = debug.toggle(player.getUuid());
            sender.sendMessage(Messages.withPrefix(Component.text(
                    watching ? "Creek debug on." : "Creek debug off.", NamedTextColor.GRAY)));
            if (watching && !debug.isActive()) {
                player.sendActionBar(CreekDebug.INACTIVE);
            }
        });
    }

    /**
     * Checks whether the sender may use this command. Works like {@code GlitchCommand}.
     *
     * @param sender the sender to check
     * @return {@code true} if the sender holds {@value #PERMISSION}, {@code false} otherwise
     */
    private static boolean hasCreekPermission(CommandSender sender) {
        return sender.getOrDefault(PermissionChecker.POINTER, PermissionChecker.always(TriState.FALSE))
                .test(PERMISSION);
    }
}
