package net.onelitefeather.cygnus.listener.view;

import net.kyori.adventure.text.Component;
import net.onelitefeather.cygnus.common.page.PageProvider;
import net.onelitefeather.cygnus.hud.PageTimerHudComponent;
import net.onelitefeather.cygnus.view.event.ViewUpdateEvent;

import java.util.function.Consumer;

public class ViewUpdateListener implements Consumer<ViewUpdateEvent> {

    private final PageTimerHudComponent pageTimerHudComponent;
    private final PageProvider pageProvider;

    public ViewUpdateListener(PageTimerHudComponent pageTimerHudComponent, PageProvider pageProvider) {
        this.pageTimerHudComponent = pageTimerHudComponent;
        this.pageProvider = pageProvider;
    }

    @Override
    public void accept(ViewUpdateEvent event) {
        Component pageStatus = this.pageProvider.getPageStatus();
        this.pageTimerHudComponent.update(event.ticks(), pageStatus);
    }
}
