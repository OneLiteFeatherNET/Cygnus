package net.onelitefeather.cygnus.common.permission;

import net.kyori.adventure.util.TriState;
import net.luckperms.api.util.Tristate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TriStatesTest {

    @Test
    void testConvertTrue() {
        assertEquals(TriState.TRUE, TriStates.fromLuckPerms(Tristate.TRUE));
    }

    @Test
    void testConvertFalse() {
        assertEquals(TriState.FALSE, TriStates.fromLuckPerms(Tristate.FALSE));
    }

    @Test
    void testConvertUndefined() {
        assertEquals(TriState.NOT_SET, TriStates.fromLuckPerms(Tristate.UNDEFINED));
    }

    @ParameterizedTest
    @EnumSource(Tristate.class)
    void testEveryValueIsMapped(Tristate tristate) {
        assertNotNull(TriStates.fromLuckPerms(tristate));
    }
}
