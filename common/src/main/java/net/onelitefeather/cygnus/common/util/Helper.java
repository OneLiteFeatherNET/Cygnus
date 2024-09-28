package net.onelitefeather.cygnus.common.util;

import de.icevizion.aves.util.Players;
import de.icevizion.xerus.api.team.Team;
import de.icevizion.xerus.api.team.TeamService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.utils.Direction;
import net.minestom.server.utils.validate.Check;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.Tags;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
public final class Helper {

    public static final byte SLENDER_ID = 0;
    public static final byte SURVIVOR_ID = 1;
    public static final long MIDNIGHT_TIME = 18000L;
    public static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final double PAGE_VISIBLE_OFFSET = -0.030;
    private static final int MIN_OFFSET = 1;
    private static final int MAX_OFFSET = 30;
    private static final float MIN_SOUND_VALUE = .1f;
    private static final float MAX_SOUND_VALUE = 1.0f;
    private static final float DEFAULT_SPEED = 0.1f;

    private Helper() {
    }

    /**
     * Prepares which player should be the slender and which players should be the survivors.
     *
     * @param slenderTeam  the team for the slender
     * @param survivorTeam the team for the survivors
     * @return the player who is the slender
     */
    public static @NotNull Player prepareTeamAllocation(@NotNull Team slenderTeam, @NotNull Team survivorTeam) {
        Check.argCondition(slenderTeam.getCapacity() != 1, "The slender team must have a capacity from one");
        var slenderPlayer = getRandomPlayer();
        var onlinePlayers = MinecraftServer.getConnectionManager().getOnlinePlayers()
                .stream().filter(player -> !player.equals(slenderPlayer)).collect(Collectors.toSet());

        slenderPlayer.setTag(Tags.TEAM_ID, SLENDER_ID);

        slenderTeam.addPlayer(slenderPlayer);
        survivorTeam.addPlayers(onlinePlayers);

        onlinePlayers.forEach(player -> player.setTag(Tags.TEAM_ID, SURVIVOR_ID));

        return slenderPlayer;
    }

    public static void updateTabList(@NotNull TeamService<Team> teamService) {
        var survivors = teamService.getTeams().get(SURVIVOR_ID);
        var slender = teamService.getTeams().getFirst();

        for (Player player : slender.getPlayers()) {
            player.setDisplayName(Messages.withMini("<red>⛧ <gray>" + player.getUsername()));
        }

        survivors.getPlayers().forEach(player -> player.setDisplayName(MINI_MESSAGE.deserialize("<green>" + player.getUsername())));
    }

    public static int calculateOffsetTime(int timeValue) {
        return timeValue + ((timeValue * SECURE_RANDOM.nextInt(MIN_OFFSET, MAX_OFFSET)) / 100);
    }

    public static float getRandomPitchValue() {
        return SECURE_RANDOM.nextFloat(MIN_SOUND_VALUE, MAX_SOUND_VALUE);
    }

    public static @NotNull Player getRandomPlayer() {
        return Players.getRandomPlayer().get();
    }

    public static int getRandomInt(int maximumValue) {
        return ThreadLocalRandom.current().nextInt(1, maximumValue);
    }

    public static void changeSpeedValue(@NotNull Player player, boolean reset) {
        player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(reset ? 0.1f : 0.13f);
        if (reset) {
            player.setSprinting(false);
            player.sendPacketToViewersAndSelf(player.getMetadataPacket());
        }
    }

    @Contract(pure = true)
    public static @NotNull Pos updatePosition(@NotNull Pos pos, @NotNull Direction direction) {
        return switch (direction) {
            case NORTH -> pos.add(0.5, .5, 1); // ?
            case SOUTH -> pos.add(0.5, .5, PAGE_VISIBLE_OFFSET); //Yes //z = 1
            case EAST -> pos.add(PAGE_VISIBLE_OFFSET, .5, 0.5).withView(-90, 0); // Yes // x = -1
            case WEST -> pos.add(1, .5, 0.5).withView(-90, 0); //?*/
            default -> throw new IllegalArgumentException("Found a direction that is not supported");
        };
    }
}
