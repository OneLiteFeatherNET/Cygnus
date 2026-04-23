package net.onelitefeather.cygnus.component;

import net.theevilreaper.xerus.api.component.ObjectComponent;

/**
 * The {@link TeamNameComponent} is a component that contains the name of the team.
 *
 * @param teamName of the team
 * @author theEvilReaper
 * @version 1.0.0
 * @since 0.1.0
 */
public record TeamNameComponent(String teamName) implements ObjectComponent {
}
