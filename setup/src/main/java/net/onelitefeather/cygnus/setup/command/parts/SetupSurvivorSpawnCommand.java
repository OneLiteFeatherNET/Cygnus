package net.onelitefeather.cygnus.setup.command.parts;

import net.kyori.adventure.text.Component;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.condition.Conditions;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.setup.util.SetupData;
import net.onelitefeather.cygnus.setup.util.SetupMessages;
import net.onelitefeather.cygnus.setup.util.SetupMode;
import net.onelitefeather.cygnus.setup.util.SetupTags;
import org.jetbrains.annotations.NotNull;

public final class SetupSurvivorSpawnCommand extends Command {

    public SetupSurvivorSpawnCommand(@NotNull SetupData setupData) {
        super("survivor");
        setCondition(Conditions::playerOnly);
        addSyntax((sender, context) -> {
            int ordinalId = sender.getTag(SetupTags.SETUP_ID_TAG);

            if (ordinalId == -1) {
                sender.sendMessage(SetupMessages.MISSING_MAP_SELECTION);
                return;
            }

            if (!SetupMode.isMode(SetupMode.GAME, ordinalId)) {
                sender.sendMessage(SetupMessages.getInvalidModeDuringLobby("survivor"));
                return;
            }

            if (setupData.hasPageMode()) {
                sender.sendMessage(SetupMessages.DISABLED_PAGE_MODE);
                return;
            }

            GameMap gameMap = (GameMap) setupData.getBaseMap();
            gameMap.addSurvivorSpawn(Pos.fromPoint(((Player) sender).getPosition()));
            sender.sendMessage(Component.text("The survivor spawn was added to the map"));
        });
    }
}
