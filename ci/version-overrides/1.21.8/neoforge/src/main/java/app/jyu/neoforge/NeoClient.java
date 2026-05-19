package app.jyu.neoforge;

import app.jyu.common.PingPoint;
import app.jyu.common.SophisticatedPingClientCommon;
import app.jyu.neoforge.platform.PlatformContextServiceImpl;
import net.neoforged.bus.api.IEventBus;

public final class NeoClient {
    public NeoClient(IEventBus modBus) {
        PlatformContextServiceImpl.modBus = modBus;
        SophisticatedPingClientCommon.init();
    }

    public static void receivePing(PingPoint point) {
        SophisticatedPingClientCommon.receivePing(point);
    }

    public static void receiveRemovePing(PingPoint point) {
        SophisticatedPingClientCommon.receiveRemovePing(point);
    }
}
