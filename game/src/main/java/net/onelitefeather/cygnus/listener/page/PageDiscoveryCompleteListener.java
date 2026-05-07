package net.onelitefeather.cygnus.listener.page;

import net.onelitefeather.cygnus.common.page.event.PageDiscoveryCompletedEvent;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.phase.GamePhase;
import net.theevilreaper.xerus.api.phase.LinearPhaseSeries;
import net.theevilreaper.xerus.api.phase.TimedPhase;

import java.util.function.Consumer;

public final class PageDiscoveryCompleteListener implements Consumer<PageDiscoveryCompletedEvent> {

    private final LinearPhaseSeries<TimedPhase> phaseSeries;

    public PageDiscoveryCompleteListener(LinearPhaseSeries<TimedPhase> phaseSeries) {
        this.phaseSeries = phaseSeries;
    }

    @Override
    public void accept(PageDiscoveryCompletedEvent event) {
        GamePhase gamePhase = (GamePhase) this.phaseSeries.getCurrentPhase();
        gamePhase.setFinishEvent(new GameFinishEvent(GameFinishEvent.Reason.ALL_PAGES_FOUND));
        gamePhase.finish();
    }
}
