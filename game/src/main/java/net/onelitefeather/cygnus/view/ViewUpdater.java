package net.onelitefeather.cygnus.view;

import net.kyori.adventure.text.Component;

@FunctionalInterface
public interface ViewUpdater {

    Component updateView();
}
