package net.theevilreaper.cygnus.cloudnet;

import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.modules.bridge.BridgeServiceHelper;

final class CloudNetCloudGameAPI implements CloudGameAPI{

    private final static BridgeServiceHelper BRIDGE_SERVICE_HELPER;
    static {
        BRIDGE_SERVICE_HELPER = InjectionLayer.ext().instance(BridgeServiceHelper.class);
    }

    @Override
    public void switchInGame() {
        BRIDGE_SERVICE_HELPER.changeToIngame();
    }
}
