package net.onelitefeather.cygnus.command.parts;

import net.kyori.adventure.text.Component;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.condition.Conditions;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.map.GameMap;
import net.onelitefeather.cygnus.setup.SetupData;
import net.onelitefeather.cygnus.setup.SetupMode;
import net.onelitefeather.cygnus.setup.SetupValidations;
import org.jetbrains.annotations.NotNull;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since
 **/

public class SetupSlenderSpawnCommand extends Command {
    public SetupSlenderSpawnCommand(@NotNull SetupData setupData) {
        super("slender");

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

            gameMap.setSlenderSpawn(Pos.fromPoint(((Player) sender).getPosition()));
            sender.sendMessage(Component.text("The slender spawn was added to the map"));
        });
    }
}
