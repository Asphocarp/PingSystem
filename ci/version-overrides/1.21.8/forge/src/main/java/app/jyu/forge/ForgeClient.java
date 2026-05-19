package app.jyu.forge;

import app.jyu.common.SophisticatedPingClientCommon;
import app.jyu.forge.platform.PlatformContextServiceImpl;
import net.minecraftforge.eventbus.api.bus.BusGroup;

public final class ForgeClient {
    public ForgeClient(BusGroup modBusGroup) {
        PlatformContextServiceImpl.modBusGroup = modBusGroup;
        SophisticatedPingClientCommon.init();
    }
}
