package net.onelitefeather.cygnus.listener.page;

import net.kyori.adventure.sound.Sound;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.sound.SoundEvent;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.page.PageProvider;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.onelitefeather.cygnus.team.TeamHelper;

import java.util.UUID;
import java.util.function.Consumer;

public final class PlayerPageInteractListener implements Consumer<PlayerEntityInteractEvent> {

    /** What only the finder hears when a page is theirs, so the click feels answered. */
    static final Sound FOUND = Sound.sound(SoundEvent.ITEM_BOOK_PAGE_TURN, Sound.Source.MASTER, 1F, 1F);

    private final PageProvider pageProvider;

    public PlayerPageInteractListener(PageProvider pageProvider) {
        this.pageProvider = pageProvider;
    }

    @Override
    public void accept(PlayerEntityInteractEvent event) {
        Player player = event.getPlayer();
        Entity target = event.getTarget();
        if (!TeamHelper.isSurvivorTeam(player) || !target.hasTag(Tags.PAGE_TAG)) return;
        UUID uuid = target.getTag(Tags.PAGE_TAG);
        if (!this.pageProvider.triggerPageFound(player, uuid)) return;
        player.playSound(FOUND, Sound.Emitter.self());
        if (player instanceof CygnusPlayer cygnusPlayer) {
            cygnusPlayer.incrementPageFound();
        }
    }
}
