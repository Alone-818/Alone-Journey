package Alone818.com.alone_journey.init;

import Alone818.com.alone_journey.Alone_journey;
import Alone818.com.alone_journey.Items.chainsword;
import Alone818.com.alone_journey.Items.core_upgrade;
import Alone818.com.alone_journey.Items.fuel_generator;
import Alone818.com.alone_journey.Items.operating_table;
import Alone818.com.alone_journey.Items.power_core;
import Alone818.com.alone_journey.Items.power_pylon;
import Alone818.com.alone_journey.Items.signal_pole;
import Alone818.com.alone_journey.Items.void_miner;
import Alone818.com.alone_journey.Items.painstrike_hammer;
import Alone818.com.alone_journey.Items.parryshield;
import Alone818.com.alone_journey.Items.powersword;
import Alone818.com.alone_journey.Itemcuiros.adaptive_flesh;
import Alone818.com.alone_journey.Itemcuiros.bleedingshield;
import Alone818.com.alone_journey.Itemcuiros.engineer_goggles;
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
    // 工程师护目镜（Curios 头部饰品）
    public static final RegistryObject<Item> ENGINEER_GOGGLES =
            ITEMS.register("engineer_goggles", () -> new engineer_goggles());
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

    // 燃料发电机方块物品
    public static final RegistryObject<Item> FUEL_GENERATOR =
            ITEMS.register("fuel_generator",
                    () -> new fuel_generator());

    // 控制核心方块物品
    public static final RegistryObject<Item> POWER_CORE =
            ITEMS.register("power_core",
                    () -> new power_core());

    // 信号杆方块物品
    public static final RegistryObject<Item> SIGNAL_POLE =
            ITEMS.register("signal_pole",
                    () -> new signal_pole());

    // 用电桩方块物品
    public static final RegistryObject<Item> POWER_PYLON =
            ITEMS.register("power_pylon",
                    () -> new power_pylon());

    // 核心升级组件物品（按等级区分：等级N的核心需要 N+1 级组件升级）
    public static final RegistryObject<Item> CORE_UPGRADE_1 =
            ITEMS.register("core_upgrade_1", () -> new core_upgrade(1));
    public static final RegistryObject<Item> CORE_UPGRADE_2 =
            ITEMS.register("core_upgrade_2", () -> new core_upgrade(2));
    public static final RegistryObject<Item> CORE_UPGRADE_3 =
            ITEMS.register("core_upgrade_3", () -> new core_upgrade(3));
    public static final RegistryObject<Item> CORE_UPGRADE_4 =
            ITEMS.register("core_upgrade_4", () -> new core_upgrade(4));
    public static final RegistryObject<Item> CORE_UPGRADE_5 =
            ITEMS.register("core_upgrade_5", () -> new core_upgrade(5));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}