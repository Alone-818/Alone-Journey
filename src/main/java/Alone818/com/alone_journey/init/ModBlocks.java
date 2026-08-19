package Alone818.com.alone_journey.init;

import Alone818.com.alone_journey.Alone_journey;
import Alone818.com.alone_journey.blocks.OperatingTable;
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

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}