package Alone818.com.alone_journey.blocks;

import Alone818.com.alone_journey.blockentities.SignalPoleBlockEntity;
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
 * 信号杆方块
 *
 * 功能特性：
 * - 整根杆为 1×1×5 分段方块（碰撞完整，见 AbstractPoleBlock）
 * - 方块实体挂在底座（PART=0），周围 5×5×5 区域为电力区域
 * - 与控制核心及其他电网节点组成电网
 * - 右键任意一段：打开电网界面（电网总电量、连接电线按键）
 * - 导线连接点在最高处方块的中心点（顶端上方）
 * - 破坏任意一段自动移除整根杆并清除所有连接
 */
public class SignalPole extends AbstractPoleBlock {

    public SignalPole() {
        super(BlockBehaviour.Properties.of()
                .strength(3.0F)
                .requiresCorrectToolForDrops()
                .lightLevel(state -> 3));
    }

    @Override
    protected BlockEntityType<?> blockEntityType() {
        return ModBlockEntities.SIGNAL_POLE.get();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        // 方块实体只在底座
        return state.getValue(PART) == 0 ? new SignalPoleBlockEntity(pos, state) : null;
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
        if (type == ModBlockEntities.SIGNAL_POLE.get()) {
            return (BlockEntityTicker<T>) (BlockEntityTicker<SignalPoleBlockEntity>)
                    (Level lvl, BlockPos pos, BlockState st, SignalPoleBlockEntity be) -> be.tick();
        }
        return null;
    }
}
