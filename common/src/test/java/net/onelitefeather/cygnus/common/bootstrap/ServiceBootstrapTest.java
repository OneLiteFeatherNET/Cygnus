package net.onelitefeather.cygnus.common.bootstrap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServiceBootstrapTest {

    @AfterEach
    void clearSystemProperties() {
        System.clearProperty("service.bind.host");
        System.clearProperty("service.bind.port");
    }

    @Test
    void testDefaultBindHost() {
        assertEquals("localhost", ServiceBootstrap.resolveBindHost());
    }

    @Test
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
}
