package net.onelitefeather.cygnus.common.map;

import net.onelitefeather.cygnus.common.config.GameConfig;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;

// Maps as path
public record MapEntry(@NotNull Path path) {

    public boolean hasMapFile() {
        return Files.exists(path.resolve(GameConfig.MAP_FILE_NAME));
    }

    public @NotNull Path getMapFile() {
        return path.resolve(GameConfig.MAP_FILE_NAME);
    }
}
