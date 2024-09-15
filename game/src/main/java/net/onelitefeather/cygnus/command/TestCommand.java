package net.onelitefeather.cygnus.command;

import de.icevizion.xerus.api.team.Team;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.Cygnus;
import net.onelitefeather.cygnus.map.GameMap;
import net.onelitefeather.cygnus.map.MapProvider;
import net.onelitefeather.cygnus.page.PageProvider;
import net.onelitefeather.cygnus.utils.ScoreboardDisplay;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since
 **/

public class TestCommand extends Command {

    private ScoreboardDisplay scoreboardDisplay;

    private boolean hidden = false;

    public TestCommand(Cygnus cygnus, @NotNull PageProvider pageProvider, @NotNull MapProvider provider, @Nullable List<Team> teamList) {
        super("test", "t");

        this.scoreboardDisplay = new ScoreboardDisplay(teamList);

        addSyntax((sender, context) -> {
            var player = (Player) sender;
            scoreboardDisplay.addPlayer(player, Helper.SURVIVOR_ID);

           /* if (player.hasTag(Tags.HIDDEN) && player.getTag(Tags.HIDDEN) == (byte) 1) {
                player.setTag(Tags.HIDDEN, (byte) 0);
                System.out.println("not Hidden for " + player.getUsername());
            } else {
                player.setTag(Tags.HIDDEN, (byte) 1);
                System.out.println("hidden for " + player.getUsername());
            }
            //Slender
            // Survivor

            //Specator X
            MinecraftServer.getConnectionManager().getOnlinePlayers()
                    .forEach(player1 -> {
                        player1.updateViewableRule();
                    });*/
      /*   MinecraftServer.getConnectionManager().getOnlinePlayers()
                    .forEach(onlinePlayer ->
                    {
                        onlinePlayer.updateViewableRule();
                    });*/
            pageProvider.loadPageData(((GameMap) provider.getActiveMap()).getPageFaces());
            pageProvider.collectStartPages(player.getInstance());
            MinecraftServer.getSchedulerManager()
                    .buildTask(() -> {
                        pageProvider.spawn();
                        player.sendMessage("Spawned");
                    })
                    .delay(2L, ChronoUnit.SECONDS).schedule();
        });
    }
}
