package net.onelitefeather.cygnus.common.util;

import de.icevizion.xerus.api.ColorData;
import de.icevizion.xerus.api.team.Team;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MicrotusExtension.class)
class HelperTest {

    @Test
    void testFailedTeamAllocation() {
        Team slenderTeam = Team.builder().name("Slender").capacity(0).colorData(ColorData.AQUA).build();
        Team survivor = Team.builder().name("Survivor").capacity(11).colorData(ColorData.AQUA).build();

        assertNotNull(slenderTeam);
        assertNotNull(survivor);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Helper.prepareTeamAllocation(slenderTeam, survivor)
        );
        assertEquals("The slender team must have a capacity from one", exception.getMessage());
    }

    @Test
    void testTeamAllocation(@NotNull Env env) {
        Instance instance = env.createFlatInstance();

        for (int i = 0; i <= 3; i++) {
            env.createPlayer(instance);
        }

        Team slenderTeam = Team.builder().name("Slender").capacity(1).colorData(ColorData.AQUA).build();
        Team survivor = Team.builder().name("Survivor").capacity(11).colorData(ColorData.AQUA).build();

        assertNotNull(slenderTeam);
        assertNotNull(survivor);

        Helper.prepareTeamAllocation(slenderTeam, survivor);

        assertEquals(1, slenderTeam.getPlayers().size());
        assertEquals(3, survivor.getPlayers().size());

        env.destroyInstance(instance, true);
    }
}
