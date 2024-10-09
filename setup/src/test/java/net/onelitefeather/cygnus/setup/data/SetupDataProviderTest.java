package net.onelitefeather.cygnus.setup.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

import static org.junit.jupiter.api.Assertions.*;

class SetupDataProviderTest {

    private static SetupDataProvider SETUP_DATA_PROVIDER;

    @BeforeAll
    static void setUp() {
        SETUP_DATA_PROVIDER = new SetupDataProvider();
    }

    @AfterEach
    void cleanUp() {
        SETUP_DATA_PROVIDER.clear();
    }

    @AfterEach
    void tearDown() {
        SETUP_DATA_PROVIDER = null;
    }



}