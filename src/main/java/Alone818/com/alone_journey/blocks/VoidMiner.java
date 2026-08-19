package Alone818.com.alone_journey.blocks;

import Alone818.com.alone_journey.blockentities.VoidMinerBlockEntity;
import Alone818.com.alone_journey.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 虚空采矿机方块
 *
 * 功能特性：
 * - 基于方块中心位置7×7范围（XZ平面），向上高5格的立方体区域
 * - 每10分钟为一个周期，自动收集周围带有ores标签的方块
 * - 产出一次对应的矿石 + 粗矿（产量为矿石数量×5）
 * - 支持右键打开机械仓库查看内容（参考箱子结构）
 * - 不需要能源，自动工作
 */
public class VoidMiner extends BaseEntityBlock {

    public VoidMiner() {
        super(BlockBehaviour.Properties.of()
                .strength(5.0F)
                .requiresCorrectToolForDrops()
                .lightLevel(state -> 7));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new VoidMinerBlockEntity(pos, state);
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
        if (type == ModBlockEntities.VOID_MINER.get()) {
            return (BlockEntityTicker<T>) (BlockEntityTicker<VoidMinerBlockEntity>)
                    (Level lvl, BlockPos pos, BlockState st, VoidMinerBlockEntity be) -> be.tick();
        }
        return null;
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @NotNull InteractionResult use(
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull Player player,
            @NotNull InteractionHand hand,
            @NotNull BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof VoidMinerBlockEntity miner)) {
            return InteractionResult.PASS;
        }

        // 右键点击时打开机械仓库界面
        // Container 实现了 MenuProvider 接口，可以直接打开
        player.openMenu((net.minecraft.world.MenuProvider) miner);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull BlockState newState,
            boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VoidMinerBlockEntity miner) {
                for (int i = 0; i < miner.getContainerSize(); i++) {
                    ItemStack stack = miner.getItem(i);
                    if (!stack.isEmpty()) {
                        net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}