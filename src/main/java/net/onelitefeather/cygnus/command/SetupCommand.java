package net.onelitefeather.cygnus.command;

import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.condition.Conditions;
import net.onelitefeather.cygnus.command.parts.SetupBuildersCommand;
import net.onelitefeather.cygnus.command.parts.SetupSlenderSpawnCommand;
import net.onelitefeather.cygnus.command.parts.SetupSurvivorSpawnCommand;
import net.onelitefeather.cygnus.setup.SetupData;
import net.onelitefeather.cygnus.command.parts.SetupPageCommand;
import net.onelitefeather.cygnus.command.parts.SetupNameCommand;
import net.onelitefeather.cygnus.command.parts.SetupSpawnCommand;
import org.jetbrains.annotations.NotNull;

public final class SetupCommand extends Command {


    public SetupCommand(@NotNull SetupData setupData) {
        super("setup");
        this.setCondition(Conditions::playerOnly);
        this.addSubcommand(new SetupNameCommand(setupData));
        this.addSubcommand(new SetupBuildersCommand(setupData));
        this.addSubcommand(new SetupSpawnCommand(setupData));
        this.addSubcommand(new SetupSurvivorSpawnCommand(setupData));
        this.addSubcommand(new SetupSlenderSpawnCommand(setupData));
        this.addSubcommand(new SetupPageCommand(setupData));
    }

}
