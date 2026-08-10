package net.onelitefeather.cygnus.listener.map;

import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.map.event.GameMapLoadedEvent;
import net.theevilreaper.aves.util.Broadcaster;

import java.util.function.Consumer;

public final class GameMapLoadedListener implements Consumer<GameMapLoadedEvent> {

    @Override
    public void accept(GameMapLoadedEvent event) {
        GameMap gameMap = event.gameMap();
        Broadcaster.broadcast(Messages.getMapAnnouncementMessage(gameMap.name(), gameMap.builders()));
    }
}
