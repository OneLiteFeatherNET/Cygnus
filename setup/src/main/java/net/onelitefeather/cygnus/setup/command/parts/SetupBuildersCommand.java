package net.onelitefeather.cygnus.setup.command.parts;

import de.icevizion.aves.map.BaseMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.condition.Conditions;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.setup.data.SetupData;
import net.onelitefeather.cygnus.setup.data.SetupDataProvider;
import net.onelitefeather.cygnus.setup.util.SetupMessages;
import net.onelitefeather.cygnus.setup.util.SetupTags;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * The command allows the set the creators of a map to a {@link BaseMap} reference.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public final class SetupBuildersCommand extends Command {

    /**
     * Creates a new instance from command class and contains also the logic to execute the command.
     *
     * @param dataProvider the involved {@link SetupDataProvider} class to get some information from it
     */
    public SetupBuildersCommand(@NotNull SetupDataProvider dataProvider) {
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
            SetupData setupData = dataProvider.getSetupData(((Player) sender));
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
