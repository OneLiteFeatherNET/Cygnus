package net.onelitefeather.cygnus.view;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ViewUpdater {

    Component updateView();
}
