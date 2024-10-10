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
import net.onelitefeather.cygnus.setup.data.SetupDataProvider;
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
     * @param dataProvider the involved {@link SetupDataProvider} class to get some information from it
     */
    public SetupCommand(@NotNull SetupDataProvider dataProvider) {
        super("setup");
        this.setCondition(Conditions::playerOnly);
        this.addSubcommand(new SetupNameCommand(dataProvider));
        this.addSubcommand(new SetupBuildersCommand(dataProvider));
        this.addSubcommand(new SetupSpawnCommand(dataProvider));
        this.addSubcommand(new SetupSurvivorSpawnCommand(dataProvider));
        this.addSubcommand(new SetupSlenderSpawnCommand(dataProvider));
        this.addSubcommand(new SetupPageCommand(dataProvider));
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
