package app.jyu.forge;

import app.jyu.common.Constants;
import app.jyu.common.SophisticatedPingCommon;
import app.jyu.forge.platform.PlatformSoundServiceImpl;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Constants.FORGE_MOD_ID)
public final class ForgeMain {
    public ForgeMain() {
        PlatformSoundServiceImpl.registerModBus(FMLJavaModLoadingContext.get().getModEventBus());
        SophisticatedPingCommon.init();
        ForgeNetwork.register();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> new ForgeClient(FMLJavaModLoadingContext.get().getModEventBus()));
    }
}
