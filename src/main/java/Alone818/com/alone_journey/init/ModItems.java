package Alone818.com.alone_journey.init;

import Alone818.com.alone_journey.Alone_journey;
import Alone818.com.alone_journey.Items.chainsword;
import Alone818.com.alone_journey.Items.operating_table;
import Alone818.com.alone_journey.Items.void_miner;
import Alone818.com.alone_journey.Items.painstrike_hammer;
import Alone818.com.alone_journey.Items.parryshield;
import Alone818.com.alone_journey.Items.powersword;
import Alone818.com.alone_journey.Itemcuiros.adaptive_flesh;
import Alone818.com.alone_journey.Itemcuiros.bleedingshield;
import Alone818.com.alone_journey.Itemcuiros.night_contract;
import Alone818.com.alone_journey.Itemcuiros.crystalline_heart;
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
    public static final RegistryObject<Item> BLEEDINGSHIELD =
            ITEMS.register("bleedingshield",()->new bleedingshield());
    public static final RegistryObject<Item> ADAPTIVE_FLESH =
            ITEMS.register("adaptive_flesh",()->new adaptive_flesh());
    public static final RegistryObject<Item> NIGHT_CONTRACT =
            ITEMS.register("night_contract",()->new night_contract());
    public static final RegistryObject<Item> PARRY_SHIELD =
            ITEMS.register("parryshield",()->new parryshield());
    public static final RegistryObject<Item> PAINSTRIKE_HAMMER =
            ITEMS.register("painstrike_hammer",()->new painstrike_hammer());
    public static final RegistryObject<Item> CHAINSWORD =
            ITEMS.register("chainsword",()->new chainsword());
    public static final RegistryObject<Item> POWERSWORD =
            ITEMS.register("powersword",()-> new powersword());

    // 手术台方块物品
    public static final RegistryObject<Item> OPERATING_TABLE =
            ITEMS.register("operating_table",
                    () -> new operating_table());

    // 虚空采矿机方块物品
    public static final RegistryObject<Item> VOID_MINER =
            ITEMS.register("void_miner",
                    () -> new void_miner());

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}