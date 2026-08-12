package net.onelitefeather.cygnus.hud;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.text.HudSegment;
import net.onelitefeather.cygnus.common.text.SpaceFont;
import net.theevilreaper.aves.util.Strings;
import net.theevilreaper.aves.util.TimeFormat;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A {@link GlobalHudComponent} that replaces the former {@code GameView}/{@code GameViewImpl}
 * pair: the single combined time+pages BossBar shown to survivors during a round.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.4.0
 */
public final class PageTimerHudComponent extends GlobalHudComponent {

    private static final TextColor MARKER_PAGES = TextColor.color(254, 254, 250);
    private static final TextColor MARKER_TIMER = TextColor.color(254, 254, 249);
    private static final char ICON_PAGE = '\ue102';
    private static final char ICON_CLOCK = '\ue101';
    private static final int PADDING_PX = 2;
    private static final int GAP_PX = 4;

    private final BossBar bossBar;

    public PageTimerHudComponent() {
        this.bossBar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
    }

    /**
     * Rebuilds the BossBar's name from the given round ticks and page status.
     *
     * @param ticks      the remaining/elapsed round ticks to format as {@code mm:ss}
     * @param pageStatus the current "found / max" page status component
     */
    public void update(int ticks, Component pageStatus) {
        Component time = Component.text(Strings.getTimeString(TimeFormat.MM_SS, ticks));
        this.bossBar.name(HudSegment.segment(ICON_PAGE, pageStatus, PADDING_PX, MARKER_PAGES)
                .append(SpaceFont.positive(GAP_PX))
                .append(HudSegment.segment(ICON_CLOCK, time, PADDING_PX, MARKER_TIMER)));
    }

    @Override
    public void addPlayer(Player player, @Nullable Consumer<Player> consumer) {
        super.addPlayer(player, p -> {
            p.showBossBar(this.bossBar);
            if (consumer != null) consumer.accept(p);
        });
    }

    @Override
    public void removePlayer(Player player, @Nullable Consumer<Player> consumer) {
        super.removePlayer(player, p -> {
            p.hideBossBar(this.bossBar);
            if (consumer != null) consumer.accept(p);
        });
    }

    @Override
    public void render() {
        // no-op: the BossBar name is pushed synchronously from update(), driven directly by
        // ViewUpdateListener on every tick - there's nothing for a separate render pass to do.
    }

    @Override
    public void hide() {
        this.visible = false;
    }
}
