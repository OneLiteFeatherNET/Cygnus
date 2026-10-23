package net.onelitefeather.cygnus.common.bootstrap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ServiceNameTest {

    @Test
    @DisplayName("The name is the CloudNet directory without its unique id")
    void nameIsTheDirectoryWithoutItsUniqueId() {
        Path directory = Path.of("temp", "services", "Slender-1_123e4567-e89b-12d3-a456-426614174000");

        assertEquals("Slender-1", ServiceName.of(directory));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "game", "temp/services/Slender-1", "Slender-1_not-a-uuid"})
    @DisplayName("A directory outside CloudNet has no name")
    void noNameOutsideCloudNet(String directory) {
        assertNull(ServiceName.of(Path.of(directory)));
    }
}
