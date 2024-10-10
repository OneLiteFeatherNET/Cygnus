package net.onelitefeather.cygnus.setup.command.parts;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentString;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.condition.Conditions;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.setup.data.SetupData;
import net.onelitefeather.cygnus.setup.data.SetupDataProvider;
import net.onelitefeather.cygnus.setup.util.SetupMessages;
import net.onelitefeather.cygnus.setup.util.SetupMode;
import net.onelitefeather.cygnus.setup.util.SetupTags;
import org.jetbrains.annotations.NotNull;

public final class SetupNameCommand extends Command {

    public SetupNameCommand(@NotNull SetupDataProvider dataProvider) {
        super("name");
        setCondition(Conditions::playerOnly);

        ArgumentString mapName = ArgumentType.String("mapName");
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
            SetupData setupData = dataProvider.getSetupData(((Player) sender));
            if (setupData == null) return;
            setupData.getBaseMap().setName(name);
            Component message = Messages.withPrefix(Component.text("The name of the map now is: ", NamedTextColor.GRAY))
                    .append(Component.text(name, NamedTextColor.AQUA));
            sender.sendMessage(message);
            setupData.triggerInventoryUpdate();
        }, mapName);
    }
}
