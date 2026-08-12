package net.onelitefeather.cygnus.hud.player;

import net.onelitefeather.cygnus.hud.HudComponent;
import net.onelitefeather.cygnus.player.CygnusPlayer;

public abstract class PersonalHudComponent implements HudComponent {

    protected final CygnusPlayer player;
    protected boolean visible = true;

    protected PersonalHudComponent(CygnusPlayer player) {
        this.player = player;
    }

    public CygnusPlayer getPlayer() {
        return player;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }
}
