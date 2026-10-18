package net.onelitefeather.cygnus.bridge;

import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.modules.bridge.BridgeServiceHelper;
import eu.cloudnetservice.wrapper.holder.ServiceInfoHolder;

/**
 * {@link ServiceStateSwitch} backed by CloudNet.
 * <p>
 * {@link BridgeServiceHelper#changeToIngame(boolean)} sets the local state to {@code INGAME} and, on
 * the first switch only, starts a new service of the own task. It only changes a local value, so
 * {@link ServiceInfoHolder#publishServiceInfoUpdate()} pushes the new state to the cluster afterwards.
 * Both are resolved lazily so a missing CloudNet surfaces as an exception the listener handles.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.18.0
 */
public final class CloudNetServiceStateSwitch implements ServiceStateSwitch {

    @Override
    public void switchToInGame() {
        InjectionLayer<?> layer = InjectionLayer.ext();
        layer.instance(BridgeServiceHelper.class).changeToIngame(true);
        layer.instance(ServiceInfoHolder.class).publishServiceInfoUpdate();
    }
}
