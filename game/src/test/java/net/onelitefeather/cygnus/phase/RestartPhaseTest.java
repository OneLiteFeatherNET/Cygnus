package net.onelitefeather.cygnus.phase;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.rank.RankTag;
import net.onelitefeather.cygnus.team.RoleIcon;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RestartPhaseTest extends CygnusPlayerTestBase {

    @Test
    void testResetDisplayNamesReplacesTheRoundRoleIconWithTheRankTag(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        player.setDisplayName(RoleIcon.SLENDER.prefix(Component.text(player.getUsername(), NamedTextColor.GRAY)));

        new RestartPhase().resetDisplayNames();

        assertEquals(RankTag.PLAYER.prefix(Component.text(player.getUsername())), player.getDisplayName(),
                "the restart lobby must show the rank tag again, not the round's role icon (LuckPerms is absent in tests, so it falls back to RankTag.PLAYER)");

        env.destroyInstance(instance, true);
    }
}
