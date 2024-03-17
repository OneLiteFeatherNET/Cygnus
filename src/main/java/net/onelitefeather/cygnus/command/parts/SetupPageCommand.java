package net.onelitefeather.cygnus.command.parts;

import net.kyori.adventure.text.Component;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.condition.Conditions;
import net.onelitefeather.cygnus.setup.SetupData;
import net.onelitefeather.cygnus.setup.SetupValidations;
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
            if (SetupValidations.mapCondition(setupData.getBaseMap(), sender)) return;
            setupData.setPageMode(!setupData.hasPageMode());

            sender.sendMessage(Component.text("Page setup is now ..."));

            if (setupData.hasPageMode()) {
                sender.sendMessage(Component.text("Don't forget to disable this mode"));
            }
        });
    }
}
