package net.onelitefeather.cygnus.setup.command.parts;

import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.condition.Conditions;
import net.onelitefeather.cygnus.setup.util.SetupData;
import net.onelitefeather.cygnus.setup.util.SetupMessages;
import net.onelitefeather.cygnus.setup.util.SetupMode;
import net.onelitefeather.cygnus.setup.util.SetupTags;
import org.jetbrains.annotations.NotNull;

public final class SetupNameCommand extends Command {

    public SetupNameCommand(@NotNull SetupData setupData) {
        super("name");
        setCondition(Conditions::playerOnly);

        var mapName = ArgumentType.String("mapName");
        addSyntax((sender, context) -> {
            int ordinalId = sender.getTag(SetupTags.SETUP_ID_TAG);

            if (ordinalId == -1) {
                sender.sendMessage(SetupMessages.MISSING_MAP_SELECTION);
                return;
            }

            String name = context.get(mapName).trim();

            if (name.isEmpty()) {
                sender.sendMessage(SetupMessages.EMPTY_NAME);
                return;
            }
            setupData.getBaseMap().setName(name);
            sender.sendMessage("The name of the map now is: " + name);
        }, mapName);
    }
}
