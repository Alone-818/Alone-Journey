package Alone818.com.alone_journey.init;

import Alone818.com.alone_journey.blockentities.OperatingTableBlockEntity;
import Alone818.com.alone_journey.blockentities.VoidMinerBlockEntity;
import Alone818.com.alone_journey.Alone_journey;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Alone_journey.MODID);

    public static final RegistryObject<BlockEntityType<OperatingTableBlockEntity>> OPERATING_TABLE =
            BLOCK_ENTITIES.register("operating_table",
                    () -> BlockEntityType.Builder.of(OperatingTableBlockEntity::new,
                            ModBlocks.OPERATING_TABLE.get()).build(null));

    public static final RegistryObject<BlockEntityType<VoidMinerBlockEntity>> VOID_MINER =
            BLOCK_ENTITIES.register("void_miner",
                    () -> BlockEntityType.Builder.of(VoidMinerBlockEntity::new,
                            ModBlocks.VOID_MINER.get()).build(null));


    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}