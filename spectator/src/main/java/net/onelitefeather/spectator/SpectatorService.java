package net.onelitefeather.spectator;

import net.minestom.server.Viewable;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface SpectatorService extends Viewable {

    @Contract(pure = true)
    static @NotNull SpectatorService create() {
        return new DefaultSpectatorService();
    }

    void clear();

    boolean isSpectator(@NotNull Player player);

    boolean hasSpectator();
}
