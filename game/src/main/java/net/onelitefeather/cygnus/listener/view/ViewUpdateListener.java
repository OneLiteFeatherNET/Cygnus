package net.onelitefeather.cygnus.listener.view;

import net.kyori.adventure.text.Component;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.page.PageProvider;
import net.onelitefeather.cygnus.phase.GamePhase;
import net.onelitefeather.cygnus.view.GameView;
import net.onelitefeather.cygnus.view.event.ViewUpdateEvent;
import net.theevilreaper.aves.util.Strings;
import net.theevilreaper.aves.util.TimeFormat;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class ViewUpdateListener implements Consumer<ViewUpdateEvent> {

    private final GameView gameView;
    private final PageProvider pageProvider;

    public ViewUpdateListener(GameView gameView, PageProvider pageProvider) {
        this.gameView = gameView;
        this.pageProvider = pageProvider;
    }

    @Override
    public void accept(ViewUpdateEvent event) {
        int ticks = event.ticks();
        Component component = Messages.getViewComponent(
                Strings.getTimeString(TimeFormat.MM_SS, ticks),
                this.pageProvider.getPageStatus()
        );
        this.gameView.updateView(component);
    }
}
