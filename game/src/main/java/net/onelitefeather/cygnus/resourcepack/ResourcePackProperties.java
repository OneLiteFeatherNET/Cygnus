package net.onelitefeather.cygnus.resourcepack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Optional;

/**
 * Resolves the {@code resourcepack.url}/{@code resourcepack.hash} system properties used to
 * configure {@link ResourcePackService}. Both must be set for the ResourcePack feature to be
 * active; if neither is set the feature is silently disabled, and if exactly one is set or the
 * URL is malformed, it is disabled with a warning.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public record ResourcePackProperties(URI url, String hash) {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcePackProperties.class);
    private static final String URL_PROPERTY = "resourcepack.url";
    private static final String HASH_PROPERTY = "resourcepack.hash";

    /**
     * Resolves the ResourcePack properties from system properties.
     *
     * @return the resolved properties, or empty if the feature should be disabled
     */
    public static Optional<ResourcePackProperties> resolve() {
        String url = System.getProperty(URL_PROPERTY);
        String hash = System.getProperty(HASH_PROPERTY);

        boolean urlPresent = url != null && !url.isBlank();
        boolean hashPresent = hash != null && !hash.isBlank();

        if (!urlPresent && !hashPresent) {
            return Optional.empty();
        }

        if (urlPresent != hashPresent) {
            LOGGER.warn("Only one of '{}'/'{}' is set. Disabling the ResourcePack feature.", URL_PROPERTY, HASH_PROPERTY);
            return Optional.empty();
        }

        URI parsedUrl;
        try {
            parsedUrl = URI.create(url);
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("'{}' is not a valid URI: '{}'. Disabling the ResourcePack feature.", URL_PROPERTY, url, exception);
            return Optional.empty();
        }

        return Optional.of(new ResourcePackProperties(parsedUrl, hash));
    }
}
