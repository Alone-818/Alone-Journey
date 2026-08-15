package Alone818.com.alone_journey.init;

import Alone818.com.alone_journey.Alone_journey;
import Alone818.com.alone_journey.Itemcuiros.*;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Alone_journey.MODID);
    public static final RegistryObject<Item> CRYSTALLINE_HEART =
            ITEMS.register("crystalline_heart",()-> new crystalline_heart());
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
