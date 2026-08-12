package net.onelitefeather.cygnus.hud.player;

import net.onelitefeather.cygnus.player.CygnusPlayer;

public class PlayerHudComponent extends PersonalHudComponent {

    public PlayerHudComponent(CygnusPlayer player) {
        super(player);
    }

    @Override
    public void render() {

    }

    @Override
    public void hide() {
        this.visible = false;
    }
}
