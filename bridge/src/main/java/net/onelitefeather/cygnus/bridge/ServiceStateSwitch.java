package net.onelitefeather.cygnus.bridge;

/**
 * Switches the running service into its in game state.
 * <p>
 * The indirection keeps {@link InGameListener} free of any CloudNet type, so it can be tested
 * without a CloudNet node.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.18.0
 */
@FunctionalInterface
public interface ServiceStateSwitch {

    /**
     * Switches the service into its in game state.
     *
     * @throws Exception if the switch fails
     */
    void switchToInGame() throws Exception;
}
