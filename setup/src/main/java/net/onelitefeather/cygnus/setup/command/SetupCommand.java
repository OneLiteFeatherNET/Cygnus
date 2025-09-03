package net.onelitefeather.cygnus.setup.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.condition.Conditions;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.setup.command.parts.SetupBuildersCommand;
import net.onelitefeather.cygnus.setup.command.parts.SetupNameCommand;
import net.onelitefeather.cygnus.setup.command.parts.SetupPageCommand;
import net.onelitefeather.cygnus.setup.command.parts.SetupSlenderSpawnCommand;
import net.onelitefeather.cygnus.setup.command.parts.SetupSpawnCommand;
import net.onelitefeather.cygnus.setup.command.parts.SetupSurvivorSpawnCommand;
import net.onelitefeather.cygnus.setup.util.SetupData;
import org.jetbrains.annotations.NotNull;

/**
 * The setup command class is the root node for all setup commands.
 * It contains all subcommands to set up the game.
 * The setup command is only available for players.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public final class SetupCommand extends Command {

    /**
     * Creates a new instance from the command class and contains also the logic to execute the command.
     *
     * @param setupData the involved {@link SetupData} class to get some information from it
     */
    public SetupCommand(@NotNull SetupData setupData) {
        super("setup");
        this.setCondition(Conditions::playerOnly);
        this.addSubcommand(new SetupNameCommand(setupData));
        this.addSubcommand(new SetupBuildersCommand(setupData));
        this.addSubcommand(new SetupSpawnCommand(setupData));
        this.addSubcommand(new SetupSurvivorSpawnCommand(setupData));
        this.addSubcommand(new SetupSlenderSpawnCommand(setupData));
        this.addSubcommand(new SetupPageCommand(setupData));
        Component helpMessage = getHelpComponent();
        this.setDefaultExecutor((sender, context) -> sender.sendMessage(helpMessage));
    }

    /**
     * Returns the help component for the setup command.
     *
     * @return the help component
     */
    private @NotNull Component getHelpComponent() {
        Component helpComponent = Messages.withPrefix(Component.text("Help for the setup commands:", NamedTextColor.GRAY))
                .append(Component.newline());

        for (int i = 0; i < getSubcommands().size(); i++) {
            Command subCommand = getSubcommands().get(i);
            Component component = Messages.withPrefix(Component.text("- ", NamedTextColor.GRAY))
                    .append(Component.text("/setup", NamedTextColor.YELLOW))
                    .append(Component.space())
                    .append(Component.text(subCommand.getName(), NamedTextColor.GREEN));

            helpComponent = helpComponent.append(component).append(Component.newline());
        }
        return helpComponent;
    }
}
