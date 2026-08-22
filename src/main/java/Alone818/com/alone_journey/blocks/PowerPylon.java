package Alone818.com.alone_journey.blocks;

import Alone818.com.alone_journey.blockentities.PowerPylonBlockEntity;
import Alone818.com.alone_journey.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 用电桩方块
 *
 * 功能特性：
 * - 整根桩为 1×1×5 分段方块（碰撞完整，见 AbstractPoleBlock）
 * - 方块实体挂在底座（PART=0），周围 11×11×11 区域为供电区域
 * - 与控制核心及其他电网节点组成电网（拉线通过各方块界面的连接按键完成）
 * - 作为拉线起点时最大连接距离 60 格（其他节点 40 格）
 * - 导线连接点在最高处方块的中心点（顶端上方）
 * - 破坏任意一段自动移除整根桩并清除所有连接
 */
public class PowerPylon extends AbstractPoleBlock {

    public PowerPylon() {
        super(BlockBehaviour.Properties.of()
                .strength(3.0F)
                .requiresCorrectToolForDrops()
                .lightLevel(state -> 3));
    }

    @Override
    protected BlockEntityType<?> blockEntityType() {
        return ModBlockEntities.POWER_PYLON.get();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        // 方块实体只在底座
        return state.getValue(PART) == 0 ? new PowerPylonBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            @NotNull Level level,
            @NotNull BlockState state,
            @NotNull BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        if (type == ModBlockEntities.POWER_PYLON.get()) {
            return (BlockEntityTicker<T>) (BlockEntityTicker<PowerPylonBlockEntity>)
                    (Level lvl, BlockPos pos, BlockState st, PowerPylonBlockEntity be) -> be.tick();
        }
        return null;
    }
}
