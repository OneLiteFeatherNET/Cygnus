package net.onelitefeather.cygnus.setup.command.parts;

import net.kyori.adventure.text.Component;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.condition.Conditions;
import net.onelitefeather.cygnus.setup.util.SetupData;
import net.onelitefeather.cygnus.setup.util.SetupMessages;
import net.onelitefeather.cygnus.setup.util.SetupTags;
import net.onelitefeather.cygnus.setup.util.SetupValidations;
import org.jetbrains.annotations.NotNull;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since
 **/

public final class SetupPageCommand extends Command {


    public SetupPageCommand(@NotNull SetupData setupData) {
        super("page");
        setCondition(Conditions::playerOnly);

        addSyntax((sender, context) -> {
            if (!sender.hasTag(SetupTags.OCCUPIED_TAG)) {
                sender.sendMessage(SetupMessages.MISSING_MAP_SELECTION);
                return;
            }
            if (SetupValidations.mapCondition(setupData.getBaseMap(), sender)) return;
            setupData.setPageMode(!setupData.hasPageMode());

            sender.sendMessage(Component.text("Page setup is now ..."));

            if (setupData.hasPageMode()) {
                sender.sendMessage(Component.text("Don't forget to disable this mode"));
            }
        });
    }
}
