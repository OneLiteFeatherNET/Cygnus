package net.onelitefeather.cygnus.setup.listener;

import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.onelitefeather.cygnus.setup.data.GameData;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import net.onelitefeather.cygnus.setup.util.SetupTags;
import net.onelitefeather.guira.SetupDataService;
import net.onelitefeather.guira.data.SetupData;

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

        gameData.setPosition(MapDataCategory.SURVIVOR, player);
    }
}
