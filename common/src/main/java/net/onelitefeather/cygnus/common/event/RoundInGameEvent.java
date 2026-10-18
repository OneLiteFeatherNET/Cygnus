package net.onelitefeather.cygnus.common.event;

import net.minestom.server.event.Event;

/**
 * The {@link RoundInGameEvent} is called once per round at the moment the round is in game.
 * <p>
 * It is a pure signal for components outside the game module, for example the CloudNet bridge
 * extension, which must not depend on the game module itself.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.18.0
 */
public final class RoundInGameEvent implements Event {
}
