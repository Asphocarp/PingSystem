package app.jyu;

import app.jyu.ClientTickHandler;
import net.minecraft.client.MinecraftClient;

public class ClientTickHandler {
    private static final ClientTickHandler INSTANCE = new ClientTickHandler();

    private ClientTickHandler() {}

    public static ClientTickHandler getInstance() {
        return INSTANCE;
    }

    public void onClientTick(MinecraftClient mc) {
        if (mc.world != null && mc.player != null) {
            RenderHandler.getInstance().updateData(mc);
        }
    }
}

