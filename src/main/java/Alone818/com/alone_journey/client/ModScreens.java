package Alone818.com.alone_journey.client;

import Alone818.com.alone_journey.Alone_journey;
import Alone818.com.alone_journey.init.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 客户端界面注册
 */
@Mod.EventBusSubscriber(modid = Alone_journey.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModScreens {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // MenuScreens.register 非线程安全，需在 enqueueWork 中执行
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.FUEL_GENERATOR.get(), FuelGeneratorScreen::new);
            MenuScreens.register(ModMenus.POWER_CORE.get(), PowerCoreScreen::new);
            MenuScreens.register(ModMenus.SIGNAL_POLE.get(), SignalPoleScreen::new);
            MenuScreens.register(ModMenus.POWER_PYLON.get(), PowerPylonScreen::new);
        });
    }
}
