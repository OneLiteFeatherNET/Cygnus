package net.onelitefeather.cygnus.setup.command.parts;

import net.kyori.adventure.text.Component;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.condition.Conditions;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.setup.util.SetupData;
import net.onelitefeather.cygnus.setup.util.SetupMode;
import net.onelitefeather.cygnus.setup.util.SetupValidations;
import org.jetbrains.annotations.NotNull;

public final class SetupSurvivorSpawnCommand extends Command {

    public SetupSurvivorSpawnCommand(@NotNull SetupData setupData) {
        super("survivor");
        setCondition(Conditions::playerOnly);
        addSyntax((sender, context) -> {
            if (SetupValidations.mapCondition(setupData.getBaseMap(), sender)) return;

            if (setupData.getSetupMode() == null || setupData.getSetupMode() != SetupMode.GAME) {
                sender.sendMessage(Component.text("The command is only allowed for game maps"));
                return;
            }

            if (setupData.hasPageMode()) {
                sender.sendMessage(Component.text("Please disable page mode to use this command"));
                return;
            }

            GameMap gameMap = (GameMap) setupData.getBaseMap();

          /*  if (gameMap.hasEnoughSurvivorSpawns()) {
                sender.sendMessage(Component.text("The map reached the maximum value of survivor spawns! Please delete some spawns"));
                return;
            }*/

            gameMap.addSurvivorSpawn(Pos.fromPoint(((Player) sender).getPosition()));
            sender.sendMessage(Component.text("The survivor spawn was added to the map"));
        });
    }
}
