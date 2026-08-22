package Alone818.com.alone_journey.init;

import Alone818.com.alone_journey.Alone_journey;
import Alone818.com.alone_journey.menus.FuelGeneratorMenu;
import Alone818.com.alone_journey.menus.PowerCoreMenu;
import Alone818.com.alone_journey.menus.PowerPylonMenu;
import Alone818.com.alone_journey.menus.SignalPoleMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, Alone_journey.MODID);

    // 燃料发电机菜单
    public static final RegistryObject<MenuType<FuelGeneratorMenu>> FUEL_GENERATOR =
            MENUS.register("fuel_generator",
                    () -> IForgeMenuType.create((windowId, inv, data) ->
                            new FuelGeneratorMenu(windowId, inv, data.readBlockPos())));

    // 控制核心菜单
    public static final RegistryObject<MenuType<PowerCoreMenu>> POWER_CORE =
            MENUS.register("power_core",
                    () -> IForgeMenuType.create((windowId, inv, data) ->
                            new PowerCoreMenu(windowId, inv, data.readBlockPos())));

    // 信号杆菜单
    public static final RegistryObject<MenuType<SignalPoleMenu>> SIGNAL_POLE =
            MENUS.register("signal_pole",
                    () -> IForgeMenuType.create((windowId, inv, data) ->
                            new SignalPoleMenu(windowId, inv, data.readBlockPos())));

    // 用电桩菜单
    public static final RegistryObject<MenuType<PowerPylonMenu>> POWER_PYLON =
            MENUS.register("power_pylon",
                    () -> IForgeMenuType.create((windowId, inv, data) ->
                            new PowerPylonMenu(windowId, inv, data.readBlockPos())));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
