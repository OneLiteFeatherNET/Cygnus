package net.onelitefeather.cygnus.command.parts;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.condition.Conditions;
import net.onelitefeather.cygnus.setup.SetupData;
import net.onelitefeather.cygnus.setup.SetupValidations;
import org.jetbrains.annotations.NotNull;

public final class SetupNameCommand extends Command {

    public SetupNameCommand(@NotNull SetupData setupData) {
        super("name");
        setCondition(Conditions::playerOnly);

        var mapName = ArgumentType.String("mapName");
        addSyntax((sender, context) -> {
            var name = context.get(mapName);
            if (SetupValidations.mapCondition(setupData.getBaseMap(), sender)) return;
            if (SetupValidations.argCondition(name.trim().isEmpty(), sender, Component.text("An empty name is not allowed", NamedTextColor.RED))) return;
            setupData.getBaseMap().setName(name);
            sender.sendMessage("The name of the map now is: " + name);
        }, mapName);
    }
}
