package net.onelitefeather.cygnus.common.config;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since
 **/

public class ConfigHolder {

    private final Path mapPath;

    public ConfigHolder(@NotNull Path path) {
        path = checkFileExists(path);
        this.mapPath = checkFileExists(path.resolve("maps"));
    }

    public @NotNull Path checkFileExists(@NotNull Path path) {
        Path updatedPath = path;
        if (!Files.exists(path)) {
            try {
                updatedPath = Files.createDirectory(path);
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        }
        return updatedPath;
    }

    public @NotNull Path getMapPath() {
        return mapPath;
    }
}
