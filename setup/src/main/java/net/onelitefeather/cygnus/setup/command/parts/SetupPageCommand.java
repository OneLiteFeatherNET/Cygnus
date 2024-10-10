package net.onelitefeather.cygnus.setup.command.parts;

import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.condition.Conditions;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.setup.data.SetupData;
import net.onelitefeather.cygnus.setup.data.SetupDataProvider;
import net.onelitefeather.cygnus.setup.util.SetupMessages;
import net.onelitefeather.cygnus.setup.util.SetupMode;
import net.onelitefeather.cygnus.setup.util.SetupTags;
import org.jetbrains.annotations.NotNull;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since
 **/
public final class SetupPageCommand extends Command {

    public SetupPageCommand(@NotNull SetupDataProvider dataProvider) {
        super("page");
        setCondition(Conditions::playerOnly);

        addSyntax((sender, context) -> {
            int ordinalId = sender.getTag(SetupTags.SETUP_ID_TAG);

            if (ordinalId == -1) {
                sender.sendMessage(SetupMessages.MISSING_MAP_SELECTION);
                return;
            }

            if (!SetupMode.isMode(SetupMode.GAME, ordinalId)) {
                sender.sendMessage(SetupMessages.getInvalidModeDuringLobby("page"));
                return;
            }

            SetupData setupData = dataProvider.getSetupData(((Player) sender));
            setupData.swapPageMode();

            if (setupData.hasPageMode()) {
                sender.sendMessage(SetupMessages.PAGE_MODE_ENABLED);
                sender.sendMessage(SetupMessages.PAGE_MODE_INFORM);
                return;
            }

            sender.sendMessage(SetupMessages.PAGE_MODE_DISABLED);
        });
    }
}
