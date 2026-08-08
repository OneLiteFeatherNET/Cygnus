package net.onelitefeather.cygnus.resourcepack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourcePackPropertiesTest {

    private static final String URL_PROPERTY = "resourcepack.url";
    private static final String HASH_PROPERTY = "resourcepack.hash";
    private static final String VALID_URL = "https://example.com/pack.zip";
    private static final String VALID_HASH = "a".repeat(40);

    @AfterEach
    void clearSystemProperties() {
        System.clearProperty(URL_PROPERTY);
        System.clearProperty(HASH_PROPERTY);
    }

    @Test
    void testResolveReturnsEmptyWhenBothPropertiesAreUnset() {
        assertTrue(ResourcePackProperties.resolve().isEmpty());
    }

    @Test
    void testResolveReturnsEmptyWhenOnlyUrlIsSet() {
        System.setProperty(URL_PROPERTY, VALID_URL);
        assertTrue(ResourcePackProperties.resolve().isEmpty());
    }

    @Test
    void testResolveReturnsEmptyWhenOnlyHashIsSet() {
        System.setProperty(HASH_PROPERTY, VALID_HASH);
        assertTrue(ResourcePackProperties.resolve().isEmpty());
    }

    @Test
    void testResolveReturnsEmptyWhenUrlIsNotAValidUri() {
        System.setProperty(URL_PROPERTY, "http://[::1");
        System.setProperty(HASH_PROPERTY, VALID_HASH);
        assertTrue(ResourcePackProperties.resolve().isEmpty());
    }

    @Test
    void testResolveReturnsPropertiesWhenBothAreSet() {
        System.setProperty(URL_PROPERTY, VALID_URL);
        System.setProperty(HASH_PROPERTY, VALID_HASH);

        Optional<ResourcePackProperties> resolved = ResourcePackProperties.resolve();

        assertTrue(resolved.isPresent());
        assertEquals(URI.create(VALID_URL), resolved.get().url());
        assertEquals(VALID_HASH, resolved.get().hash());
    }
}
