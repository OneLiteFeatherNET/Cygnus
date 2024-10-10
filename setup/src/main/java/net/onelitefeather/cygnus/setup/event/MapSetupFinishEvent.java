package net.onelitefeather.cygnus.setup.event;

import net.minestom.server.event.Event;
import net.onelitefeather.cygnus.setup.data.SetupData;
import org.jetbrains.annotations.NotNull;

public record MapSetupFinishEvent(@NotNull SetupData setupData) implements Event {
}
