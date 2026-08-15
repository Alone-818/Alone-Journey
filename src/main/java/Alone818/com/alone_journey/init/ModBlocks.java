package Alone818.com.alone_journey.init;

import Alone818.com.alone_journey.Alone_journey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Alone_journey.MODID);




    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
