package net.onelitefeather.cygnus.stats;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Covers the switched-off repository that is used while no statistics database is configured.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.21.0
 */
class NoOpSlenderPassRepositoryTest {

    @Test
    void reportsTheFeatureAsUnavailable() {
        assertFalse(new NoOpSlenderPassRepository().available());
    }

    @Test
    void neverSpendsAPass() {
        assertFalse(new NoOpSlenderPassRepository().redeem(UUID.randomUUID(), UUID.randomUUID()).join());
    }

    @Test
    void reportsAZeroBalanceAndNoChange() {
        NoOpSlenderPassRepository repository = new NoOpSlenderPassRepository();
        UUID player = UUID.randomUUID();

        assertEquals(0, repository.balance(player).join());
        assertEquals(0, repository.give(player, 5).join().delta());
    }
}
