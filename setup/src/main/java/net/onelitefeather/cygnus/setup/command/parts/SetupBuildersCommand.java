package net.onelitefeather.cygnus.setup.command.parts;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.condition.Conditions;
import net.onelitefeather.cygnus.setup.util.SetupData;
import net.onelitefeather.cygnus.setup.util.SetupMessages;
import net.onelitefeather.cygnus.setup.util.SetupTags;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * The command allows the set the creators of a map to a {@link de.icevizion.aves.map.BaseMap} reference.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public final class SetupBuildersCommand extends Command {

    /**
     * Creates a new instance from command class and contains also the logic to execute the command.
     *
     * @param setupData the involved {@link SetupData} class to get some information from it
     */
    public SetupBuildersCommand(@NotNull SetupData setupData) {
        super("builders");
        setCondition(Conditions::playerOnly);

        var buildersArray = ArgumentType.StringArray("builders");

        addSyntax((sender, context) -> {
            int ordinalId = sender.getTag(SetupTags.SETUP_ID_TAG);

            if (ordinalId == -1) {
                sender.sendMessage(SetupMessages.MISSING_MAP_SELECTION);
                return;
            }
            String[] builders = context.get(buildersArray);

            if (builders.length == 0) {
                sender.sendMessage(Component.text("A map needs at least one builder", NamedTextColor.RED));
                return;
            }
            setupData.getBaseMap().setBuilders(builders);
            var buildersAsComponent = Component.join(JoinConfiguration.arrayLike(), transformBuilders(builders));
            sender.sendMessage(Component.text("The creators of the map are: ").append(buildersAsComponent));
            setupData.triggerInventoryUpdate();
        }, buildersArray);
    }

    /**
     * Transforms the given builders into a {@link TextComponent} list.
     *
     * @param builders the builders to transform
     * @return a list with the transformed builders
     */
    private @NotNull List<TextComponent> transformBuilders(@NotNull String... builders) {
        return Arrays.stream(builders).map(Component::text).toList();
    }
}
