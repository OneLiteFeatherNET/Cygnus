package net.onelitefeather.cygnus.common.bootstrap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServiceBootstrapTest {

    @AfterEach
    void clearSystemProperties() {
        System.clearProperty("service.bind.host");
        System.clearProperty("service.bind.port");
    }

    @Test
    @DisabledIfSystemProperty(named = "service.bind.host", matches = ".+")
    void testDefaultBindHost() {
        assertEquals("localhost", ServiceBootstrap.resolveBindHost());
    }

    @Test
    @DisabledIfSystemProperty(named = "service.bind.port", matches = ".+")
    void testDefaultBindPort() {
        assertEquals(25565, ServiceBootstrap.resolveBindPort());
    }

    @Test
    void testBindHostFromSystemProperty() {
        System.setProperty("service.bind.host", "0.0.0.0");
        assertEquals("0.0.0.0", ServiceBootstrap.resolveBindHost());
    }

    @Test
    void testBindPortFromSystemProperty() {
        System.setProperty("service.bind.port", "30000");
        assertEquals(30000, ServiceBootstrap.resolveBindPort());
    }

    @AfterEach
    void clearWorkingDirProperty() {
        System.clearProperty("service.working.dir");
    }

    @Test
    @DisabledIfSystemProperty(named = "service.working.dir", matches = ".+")
    void testDefaultWorkingDirectory() {
        assertEquals(Paths.get(""), ServiceBootstrap.resolveWorkingDirectory());
    }

    @Test
    void testWorkingDirectoryFromSystemProperty() {
        System.setProperty("service.working.dir", "/app");
        assertEquals(Paths.get("/app"), ServiceBootstrap.resolveWorkingDirectory());
    }
}
