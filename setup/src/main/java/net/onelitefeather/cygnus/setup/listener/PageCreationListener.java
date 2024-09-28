package net.onelitefeather.cygnus.setup.listener;

import de.icevizion.aves.util.Components;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.utils.Direction;
import net.minestom.server.utils.MathUtils;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.util.DirectionFaceHelper;
import net.onelitefeather.cygnus.setup.util.SetupData;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class PageCreationListener implements Consumer<PlayerBlockBreakEvent> {

    private final SetupData setupData;

    public PageCreationListener(@NotNull SetupData setupData) {
        this.setupData = setupData;
    }

    @Override
    public void accept(@NotNull PlayerBlockBreakEvent event) {
        event.setCancelled(true);

        if (setupData.getBaseMap() == null || !setupData.hasPageMode()) return;

        Player player = event.getPlayer();
        Direction direction = MathUtils.getHorizontalDirection(player.getPosition().yaw());

        Vec dir = player.getPosition().direction();

        double directionPitch = Math.toDegrees(-Math.atan2(dir.y(), Math.sqrt(dir.x() * dir.x() + dir.z() * dir.z())));

        if (!DirectionFaceHelper.isValidFace(directionPitch)) {
            String indirectDirection = DirectionFaceHelper.getInvalidDirection(directionPitch).name();
            Component invalidFace = Component.text(indirectDirection, NamedTextColor.RED);
            Component invalidComponent = Messages.withPrefix(
                    Component.text("You are looking in an invalid direction! (")
                            .append(invalidFace)
                            .append(Component.text(")"))
            );
            player.sendMessage(invalidComponent);
            return;
        }

        Vec position = Vec.fromPoint(event.getBlockPosition());
        ((GameMap) setupData.getBaseMap()).addPage(position, direction.name());

        Component component = Component.text("Created page at: ", NamedTextColor.GRAY)
                .append(Components.convertPoint(position).style(Style.style(NamedTextColor.GOLD)))
                .append(Component.text(" with direction: ", NamedTextColor.GRAY))
                .append(Component.text(direction.name(), NamedTextColor.GREEN));
        player.sendMessage(Messages.withPrefix(component));
    }
}
