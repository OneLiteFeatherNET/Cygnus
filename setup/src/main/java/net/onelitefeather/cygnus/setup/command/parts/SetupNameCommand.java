package net.onelitefeather.cygnus.setup.command.parts;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.condition.Conditions;
import net.onelitefeather.cygnus.setup.util.SetupData;
import net.onelitefeather.cygnus.setup.util.SetupMessages;
import net.onelitefeather.cygnus.setup.util.SetupTags;
import net.onelitefeather.cygnus.setup.util.SetupValidations;
import org.jetbrains.annotations.NotNull;

public final class SetupNameCommand extends Command {

    public SetupNameCommand(@NotNull SetupData setupData) {
        super("name");
        setCondition(Conditions::playerOnly);

        var mapName = ArgumentType.String("mapName");
        addSyntax((sender, context) -> {
            if (!sender.hasTag(SetupTags.OCCUPIED_TAG)) {
                sender.sendMessage(SetupMessages.MISSING_MAP_SELECTION);
                return;
            }
            var name = context.get(mapName);
            if (SetupValidations.mapCondition(setupData.getBaseMap(), sender)) return;
            if (SetupValidations.argCondition(name.trim().isEmpty(), sender, Component.text("An empty name is not allowed", NamedTextColor.RED))) return;
            setupData.getBaseMap().setName(name);
            sender.sendMessage("The name of the map now is: " + name);
        }, mapName);
    }
}
