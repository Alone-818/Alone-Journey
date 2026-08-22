package Alone818.com.alone_journey.blocks;

import Alone818.com.alone_journey.blockentities.PowerCoreBlockEntity;
import Alone818.com.alone_journey.blockentities.PowerLinkHelper;
import Alone818.com.alone_journey.blockentities.PowerLinkable;
import Alone818.com.alone_journey.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 控制核心方块
 *
 * 功能特性：
 * - 创建、控制、管理一个电力网络
 * - 右键：打开电网界面（电网信息、核心升级、连接按键）
 * - 电力连接在界面内通过"电网连接"按键完成
 * - 破坏时自动清除所有连接
 */
public class PowerCore extends BaseEntityBlock {

    // 碰撞占位3×3×3（以方块为中心向四周各扩展一格）
    // 注意：仅用于物理碰撞，选中外框/射线拾取保持本格大小，
    // 否则点击扩展区域会被服务端反作弊校验拒绝（UseItemOnPacket rejected）
    private static final VoxelShape COLLISION_SHAPE =
            Block.box(-16.0, -16.0, -16.0, 32.0, 32.0, 32.0);

    public PowerCore() {
        super(BlockBehaviour.Properties.of()
                .strength(5.0F)
                .requiresCorrectToolForDrops()
                .lightLevel(state -> 5));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new PowerCoreBlockEntity(pos, state);
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
        if (type == ModBlockEntities.POWER_CORE.get()) {
            return (BlockEntityTicker<T>) (BlockEntityTicker<PowerCoreBlockEntity>)
                    (Level lvl, BlockPos pos, BlockState st, PowerCoreBlockEntity be) -> be.tick();
        }
        return null;
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(
            @NotNull BlockState state,
            @NotNull BlockGetter level,
            @NotNull BlockPos pos,
            @NotNull CollisionContext context) {
        return COLLISION_SHAPE;
    }

    @Override
    public @NotNull InteractionResult use(
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull Player player,
            @NotNull InteractionHand hand,
            @NotNull BlockHitResult hit) {
        // 潜行右键：取消进行中的电力连接
        if (player.isShiftKeyDown() && !level.isClientSide()
                && PowerLinkHelper.cancelPendingLink(player)) {
            return InteractionResult.CONSUME;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // 右键：打开电网界面（电力连接在界面内通过按键完成）
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PowerCoreBlockEntity core && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, core, pos);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull BlockState newState,
            boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            // 清除所有连接（双向移除）+ 掉落内部物品
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PowerLinkable linkable) {
                for (BlockPos conn : linkable.getConnections()) {
                    BlockEntity other = level.getBlockEntity(conn);
                    if (other instanceof PowerLinkable otherNode) {
                        otherNode.removeConnection(pos);
                    }
                }
            }
            if (be instanceof PowerCoreBlockEntity core) {
                core.dropInventory(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
