package Alone818.com.alone_journey.init;

import Alone818.com.alone_journey.Alone_journey;
import Alone818.com.alone_journey.blocks.FuelGenerator;
import Alone818.com.alone_journey.blocks.OperatingTable;
import Alone818.com.alone_journey.blocks.PowerCore;
import Alone818.com.alone_journey.blocks.PowerPylon;
import Alone818.com.alone_journey.blocks.SignalPole;
import Alone818.com.alone_journey.blocks.VoidMiner;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Alone_journey.MODID);

    // 手术台方块
    public static final RegistryObject<Block> OPERATING_TABLE =
            BLOCKS.register("operating_table", OperatingTable::new);

    // 虚空采矿机方块
    public static final RegistryObject<Block> VOID_MINER =
            BLOCKS.register("void_miner", VoidMiner::new);

    // 燃料发电机方块
    public static final RegistryObject<Block> FUEL_GENERATOR =
            BLOCKS.register("fuel_generator", FuelGenerator::new);

    // 控制核心方块
    public static final RegistryObject<Block> POWER_CORE =
            BLOCKS.register("power_core", PowerCore::new);

    // 信号杆方块
    public static final RegistryObject<Block> SIGNAL_POLE =
            BLOCKS.register("signal_pole", SignalPole::new);

    // 用电桩方块
    public static final RegistryObject<Block> POWER_PYLON =
            BLOCKS.register("power_pylon", PowerPylon::new);


    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}