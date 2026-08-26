package net.onelitefeather.cygnus.common.bootstrap;

import net.minestom.server.Auth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ServiceBootstrapTest {

    @AfterEach
    void clearSystemProperties() {
        System.clearProperty("service.bind.host");
        System.clearProperty("service.bind.port");
    }

    @AfterEach
    void clearVelocitySecretProperty() {
        System.clearProperty(ServiceBootstrap.VELOCITY_SECRET_PROPERTY);
    }

    @Test
    @DisabledIfSystemProperty(named = ServiceBootstrap.VELOCITY_SECRET_PROPERTY, matches = ".+")
    void testAuthDefaultsToOfflineWithoutVelocitySecret() {
        assertInstanceOf(Auth.Offline.class, ServiceBootstrap.resolveAuth());
    }

    @Test
    void testAuthUsesVelocityWhenSecretIsSet() {
        System.setProperty(ServiceBootstrap.VELOCITY_SECRET_PROPERTY, "a-velocity-secret");

        Auth auth = ServiceBootstrap.resolveAuth();

        assertInstanceOf(Auth.Velocity.class, auth);
        assertEquals(new Auth.Velocity("a-velocity-secret"), auth);
    }

    @Test
    void testAuthTrimsVelocitySecret() {
        System.setProperty(ServiceBootstrap.VELOCITY_SECRET_PROPERTY, "  a-velocity-secret\n");

        assertEquals(new Auth.Velocity("a-velocity-secret"), ServiceBootstrap.resolveAuth());
    }

    @Test
    void testAuthFallsBackToOfflineOnBlankVelocitySecret() {
        // A start script whose $VELOCITY_SECRET never expanded - Auth.Velocity would reject the
        // empty key with an exception that says nothing about the property it came from.
        System.setProperty(ServiceBootstrap.VELOCITY_SECRET_PROPERTY, "   ");

        assertInstanceOf(Auth.Offline.class, ServiceBootstrap.resolveAuth());
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
