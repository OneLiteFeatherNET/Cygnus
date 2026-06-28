package net.onelitefeather.cygnus.setup.listener;

import net.onelitefeather.cygnus.common.map.GameMapBuilder;
import net.onelitefeather.cygnus.setup.data.GameData;
import net.onelitefeather.cygnus.setup.util.SetupTags;
import net.onelitefeather.guira.SetupDataService;
import net.onelitefeather.guira.data.SetupData;
import net.theevilreaper.aves.util.Components;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.utils.Direction;
import net.minestom.server.utils.MathUtils;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.util.DirectionFaceHelper;
import net.onelitefeather.cygnus.setup.util.SetupMessages;

import java.util.function.Consumer;

public final class PageCreationListener implements Consumer<PlayerBlockBreakEvent> {

    private final SetupDataService setupData;

    public PageCreationListener(SetupDataService setupData) {
        this.setupData = setupData;
    }

    @Override
    public void accept(PlayerBlockBreakEvent event) {
        event.setCancelled(true);

        Player player = event.getPlayer();

        if (!player.hasTag(SetupTags.SETUP_ID_TAG)) return;

        SetupData setupData = this.setupData.get(player.getUuid()).orElse(null);

        if (setupData == null) return;

        if (!(setupData instanceof GameData gameData) || !gameData.hasPageMode()) return;

        Direction direction = MathUtils.getHorizontalDirection(player.getPosition().yaw());

        Vec dir = player.getPosition().direction();

        double directionPitch = Math.toDegrees(-Math.atan2(dir.y(), Math.sqrt(dir.x() * dir.x() + dir.z() * dir.z())));

        if (!DirectionFaceHelper.isValidFace(directionPitch)) {
            String indirectDirection = DirectionFaceHelper.getInvalidDirection(directionPitch).name();
            player.sendMessage(SetupMessages.getInvalidFace(indirectDirection));
            return;
        }

        Vec position = event.getBlockPosition().asVec();
        ((GameMapBuilder) gameData.getMapBuilder()).addPage(position, direction);

        Component component = Component.text("Created page at: ", NamedTextColor.GRAY)
                .append(Components.convertPoint(position).style(Style.style(NamedTextColor.GOLD)))
                .append(Component.text(" with direction: ", NamedTextColor.GRAY))
                .append(Component.text(direction.name(), NamedTextColor.GREEN));
        player.sendMessage(Messages.withPrefix(component));
    }
}
