package net.onelitefeather.cygnus.listener.page;

import net.minestom.server.instance.Instance;
import net.onelitefeather.cygnus.common.page.PageProvider;
import net.onelitefeather.cygnus.common.page.event.PageSpawnEvent;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Places the start pages into the active instance once the game has started. The pages are
 * collected beforehand, see {@code GamePreLaunchListener}.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.6.7
 */
public final class PageSpawnListener implements Consumer<PageSpawnEvent> {

    private final PageProvider pageProvider;
    private final Supplier<Instance> activeInstanceSupplier;

    public PageSpawnListener(PageProvider pageProvider, Supplier<Instance> activeInstanceSupplier) {
        this.pageProvider = pageProvider;
        this.activeInstanceSupplier = activeInstanceSupplier;
    }

    @Override
    public void accept(PageSpawnEvent event) {
        Instance activeInstance = this.activeInstanceSupplier.get();
        if (activeInstance == null) {
            throw new IllegalStateException("Active instance not available for page spawning");
        }
        this.pageProvider.spawn(activeInstance);
    }
}
