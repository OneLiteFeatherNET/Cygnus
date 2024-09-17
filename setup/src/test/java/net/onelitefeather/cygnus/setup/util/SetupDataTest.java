package net.onelitefeather.cygnus.setup.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SetupDataTest {

    @Test
    void testSetupDataWithNoData() {
        SetupData setupData = new SetupData();
        assertNotNull(setupData);
        assertFalse(setupData.hasPageMode());
        assertNull(setupData.getSelectedMap());
        assertNull(setupData.getBaseMap());
        assertNull(setupData.getSetupMode());
    }

    @Test
    void testSetupModeChangeInData() {
        SetupData setupData = new SetupData();
        assertNotNull(setupData);
        assertNull(setupData.getSetupMode());
        setupData.setSetupMode(SetupMode.GAME);
        assertNotNull(setupData.getSetupMode());
        assertEquals(SetupMode.GAME, setupData.getSetupMode());
    }

    @Test
    void testSetupDataWithPageContext() {
        SetupData setupData = new SetupData();
        assertNotNull(setupData);
        assertFalse(setupData.hasPageMode());

        setupData.setPageMode(true);

        assertTrue(setupData.hasPageMode());
    }
}
