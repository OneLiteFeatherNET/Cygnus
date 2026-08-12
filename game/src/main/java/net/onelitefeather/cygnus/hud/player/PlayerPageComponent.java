package net.onelitefeather.cygnus.hud.player;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.onelitefeather.cygnus.common.text.HudSegment;
import net.onelitefeather.cygnus.common.text.SpaceFont;
import net.onelitefeather.cygnus.player.CygnusPlayer;

public class PlayerPageComponent extends PersonalHudComponent {

    private static final TextColor MARKER_PAGE_COUNT = TextColor.color(254, 254, 248);
    private static final char ICON_PAGE = '\ue103';
    private static final int PADDING_PX = 2;
    private static final int GAP_PX = 4;

    private final BossBar bossBar;

    public PlayerPageComponent(CygnusPlayer player) {
        super(player);
        this.bossBar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
        render();
        player.showBossBar(this.bossBar);
    }

    @Override
    public void render() {
        Component pages = Component.text(player.getPageCount());
        this.bossBar.name(HudSegment.segment(ICON_PAGE, pages, PADDING_PX, MARKER_PAGE_COUNT)
                .append(SpaceFont.positive(GAP_PX)));
    }

    @Override
    public void hide() {
        this.visible = false;
    }
}
