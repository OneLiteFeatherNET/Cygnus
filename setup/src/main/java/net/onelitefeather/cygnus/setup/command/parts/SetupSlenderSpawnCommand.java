package net.onelitefeather.cygnus.setup.command.parts;

import net.kyori.adventure.text.Component;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.condition.Conditions;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.map.GameMapBuilder;
import net.onelitefeather.cygnus.setup.util.SetupData;
import net.onelitefeather.cygnus.setup.util.SetupMessages;
import net.onelitefeather.cygnus.setup.util.SetupMode;
import net.onelitefeather.cygnus.setup.util.SetupTags;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since 0.1.0
 **/
public class SetupSlenderSpawnCommand extends Command {

    public SetupSlenderSpawnCommand(SetupData setupData) {
        super("slender");

        setCondition(Conditions::playerOnly);
        addSyntax((sender, context) -> {
            int ordinalId = sender.getTag(SetupTags.SETUP_ID_TAG);

            if (ordinalId == -1) {
                sender.sendMessage(SetupMessages.MISSING_MAP_SELECTION);
                return;
            }

            if (!SetupMode.isMode(SetupMode.GAME, ordinalId)) {
                sender.sendMessage(SetupMessages.getInvalidModeDuringLobby("slender"));
                return;
            }

            if (setupData.hasPageMode()) {
                sender.sendMessage(SetupMessages.DISABLED_PAGE_MODE);
                return;
            }

            GameMapBuilder gameMap = (GameMapBuilder) setupData.getBaseMapBuilder();

            Pos position = ((Player) sender).getPosition().asPos();
            gameMap.setSlenderSpawn(position);
            sender.sendMessage(Component.text("The slender spawn was added to the map"));
        });
    }
}
