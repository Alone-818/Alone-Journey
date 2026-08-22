package Alone818.com.alone_journey.blocks;

import Alone818.com.alone_journey.blockentities.FuelGeneratorBlockEntity;
import Alone818.com.alone_journey.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 燃料发电机方块
 *
 * 功能特性：
 * - 消耗燃料产生FE电力
 * - 内部储能 100,000 FE，可通过线缆/其他机器提取
 * - 右键打开发电机界面（燃料槽 + 4个升级槽 + 电量显示）
 */
public class FuelGenerator extends BaseEntityBlock {

    public FuelGenerator() {
        super(BlockBehaviour.Properties.of()
                .strength(5.0F)
                .requiresCorrectToolForDrops());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new FuelGeneratorBlockEntity(pos, state);
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
        if (type == ModBlockEntities.FUEL_GENERATOR.get()) {
            return (BlockEntityTicker<T>) (BlockEntityTicker<FuelGeneratorBlockEntity>)
                    (Level lvl, BlockPos pos, BlockState st, FuelGeneratorBlockEntity be) -> be.tick();
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
        if (!(be instanceof FuelGeneratorBlockEntity generator)) {
            return InteractionResult.PASS;
        }

        // 右键点击时打开发电机界面（通过 NetworkHooks 把方块位置写入网络数据）
        NetworkHooks.openScreen((ServerPlayer) player, generator, pos);
        return InteractionResult.CONSUME;
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
            if (be instanceof FuelGeneratorBlockEntity generator) {
                generator.dropInventory(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
