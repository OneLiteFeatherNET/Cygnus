package net.onelitefeather.cygnus.common.bootstrap;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Derives the name of the CloudNet service the game runs as.
 * <p>
 * CloudNet starts a temporary service in {@code temp/services/<name>_<unique id>} and does not hand
 * the name to the JVM in any system property, so the directory is the one place it shows up.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.19.0
 */
public final class ServiceName {

    // CloudNet names the directory of a temporary service <name>_<unique id>.
    private static final Pattern SERVICE_DIRECTORY =
            Pattern.compile("(.+)_[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}");

    private ServiceName() {
    }

    /**
     * Derives the CloudNet service name from the service directory.
     *
     * @param workingDirectory the service working directory
     * @return the service name, or {@code null} when the directory does not look like one of CloudNet's
     * (standalone runs, static services)
     */
    public static @Nullable String of(Path workingDirectory) {
        Path fileName = workingDirectory.toAbsolutePath().normalize().getFileName();
        if (fileName == null) {
            return null;
        }
        Matcher matcher = SERVICE_DIRECTORY.matcher(fileName.toString());
        return matcher.matches() ? matcher.group(1) : null;
    }
}
