package net.onelitefeather.cygnus.ambient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AmbientProviderTest {

    @Test
    void testAmbientProviderWithoutATeam() {
        AmbientProvider provider = new AmbientProvider();
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                provider::startTask
        );
        assertEquals("The team variable can't be null", exception.getMessage());
    }
}
