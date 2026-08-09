package net.onelitefeather.cygnus.setup.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.map.GameMapBuilder;
import net.onelitefeather.cygnus.setup.data.GameData;
import net.onelitefeather.cygnus.setup.data.InstanceSetupData;
import net.onelitefeather.cygnus.setup.util.SetupTags;
import net.onelitefeather.guira.SetupDataService;
import net.onelitefeather.guira.data.SetupData;
import net.theevilreaper.aves.util.Components;

import java.util.function.Consumer;

public class SpawnCreationListener implements Consumer<PlayerBlockBreakEvent> {

    private final SetupDataService setupService;

    public SpawnCreationListener(SetupDataService setupService) {
        this.setupService = setupService;
    }

    @Override
    public void accept(PlayerBlockBreakEvent event) {
        event.setCancelled(true);

        Player player = event.getPlayer();

        if (!player.hasTag(SetupTags.SETUP_ID_TAG)) return;

        SetupData setupData = this.setupService.get(player.getUuid()).orElse(null);

        if (setupData == null) return;

        if (!(setupData instanceof GameData gameData) || !gameData.hasSurvivorMode()) return;

        Vec blockPos = event.getBlockPosition().asVec();
        Pos spawnPos = new Pos(
                blockPos.x() + 0.5,
                blockPos.y() + 1.0,
                blockPos.z() + 0.5,
                player.getPosition().yaw(),
                0f
        );

        if (((GameMapBuilder) gameData.getMapBuilder()).addSurvivorSpawn(spawnPos)) {
            gameData.triggerUpdate(InstanceSetupData.InventoryTarget.SURVIVOR);
            Component component = Component.text("Created survivor spawn at: ", NamedTextColor.GRAY)
                    .append(Components.convertPoint(spawnPos).style(Style.style(NamedTextColor.GOLD)));
            player.sendMessage(Messages.withPrefix(component));
        } else {
            Component component = Component.text("Survivor spawn already exists at this block!", NamedTextColor.RED);
            player.sendMessage(Messages.withPrefix(component));
        }
    }
}
