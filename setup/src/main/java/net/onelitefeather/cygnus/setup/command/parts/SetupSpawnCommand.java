package net.onelitefeather.cygnus.setup.command.parts;

import de.icevizion.aves.util.Components;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.builder.Command;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.setup.util.SetupData;
import net.onelitefeather.cygnus.setup.util.SetupMessages;
import net.onelitefeather.cygnus.setup.util.SetupTags;
import net.onelitefeather.cygnus.setup.util.SetupValidations;
import org.jetbrains.annotations.NotNull;

public final class SetupSpawnCommand extends Command {

    public SetupSpawnCommand(@NotNull SetupData setupData) {
        super("spawn");

        addSyntax((sender, context) -> {
            if (!sender.hasTag(SetupTags.OCCUPIED_TAG)) {
                sender.sendMessage(SetupMessages.MISSING_MAP_SELECTION);
                return;
            }
            if (SetupValidations.mapCondition(setupData.getBaseMap(), sender)) return;
            if (setupData.hasPageMode()) {
                sender.sendMessage(Component.text("Please disable page mode to use this command"));
                return;
            }
            Pos position = Pos.fromPoint(((Player) sender).getPosition());
            setupData.getBaseMap().setSpawn(position);
            var posAsComponent = Components.convertPoint(position);
            sender.sendMessage(Component.text("The spawn position of the map is now located at: ").append(posAsComponent));
        });
    }
}
