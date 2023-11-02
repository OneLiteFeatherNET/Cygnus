package net.onelitefeather.cygnus.listener.setup;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.utils.MathUtils;
import net.onelitefeather.cygnus.map.GameMap;
import net.onelitefeather.cygnus.setup.SetupData;
import net.onelitefeather.cygnus.utils.Helper;
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

        var player = event.getPlayer();
        var direction = MathUtils.getHorizontalDirection(player.getPosition().yaw());

        Vec dir = player.getPosition().direction();

        var directionPitch = Math.toDegrees(-Math.atan2(dir.y(), Math.sqrt(dir.x() * dir.x() + dir.z() * dir.z())));

        if (!Helper.isValidFace(directionPitch)) {
            var invalidFace = Component.text(Helper.getInvalidDirection(directionPitch).name(), NamedTextColor.RED);
            var invalidComponent = Component.text("You are looking in an invalid direction! (").append(invalidFace).append(Component.text(")"));
            player.sendMessage(invalidComponent);
            return;
        }

        var position = Vec.fromPoint(event.getBlockPosition());
        ((GameMap) setupData.getBaseMap()).addPage(position, direction.name());

        var component = Component.text("Created page at: ")
                .append(Helper.convertPointToComponent(position))
                .append(Component.text(" with direction: "))
                .append(Component.text(direction.name(), NamedTextColor.GREEN));
        player.sendMessage(component);
    }
}
