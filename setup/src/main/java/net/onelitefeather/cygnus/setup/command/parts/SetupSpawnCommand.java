package net.onelitefeather.cygnus.setup.command.parts;

import net.theevilreaper.aves.util.Components;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.builder.Command;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.setup.util.SetupData;
import net.onelitefeather.cygnus.setup.util.SetupMessages;
import net.onelitefeather.cygnus.setup.util.SetupTags;
import org.jetbrains.annotations.NotNull;

public final class SetupSpawnCommand extends Command {

    public SetupSpawnCommand(@NotNull SetupData setupData) {
        super("spawn");

        addSyntax((sender, context) -> {
            int ordinalId = sender.getTag(SetupTags.SETUP_ID_TAG);

            if (ordinalId == -1) {
                sender.sendMessage(SetupMessages.MISSING_MAP_SELECTION);
                return;
            }

            if (setupData.hasPageMode()) {
                sender.sendMessage(SetupMessages.DISABLED_PAGE_MODE);
                return;
            }

            Pos position = Pos.fromPoint(((Player) sender).getPosition());
            setupData.getBaseMap().setSpawn(position);
            var posAsComponent = Components.convertPoint(position);
            sender.sendMessage(Component.text("The spawn position of the map is now located at: ").append(posAsComponent));
        });
    }
}
