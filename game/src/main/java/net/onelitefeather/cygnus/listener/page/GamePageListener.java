package net.onelitefeather.cygnus.listener.page;

import net.onelitefeather.cygnus.common.page.PageProvider;
import net.onelitefeather.cygnus.common.page.event.PageExpiredEvent;

import java.util.function.Consumer;

public final class GamePageListener implements Consumer<PageExpiredEvent> {

    private final PageProvider pageProvider;

    public GamePageListener(PageProvider pageProvider) {
        this.pageProvider = pageProvider;
    }

    @Override
    public void accept(PageExpiredEvent event) {
        this.pageProvider.triggerTTLHandling(event.entity().getHitBoxUUID());
    }
}
