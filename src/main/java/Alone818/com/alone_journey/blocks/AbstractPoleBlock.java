package Alone818.com.alone_journey.blocks;

import Alone818.com.alone_journey.blockentities.PowerLinkHelper;
import Alone818.com.alone_journey.blockentities.PowerLinkable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 高杆分段方块基类（信号杆/用电桩共用）
 *
 * 整根杆由 HEIGHT 个真实方块状态组成（PART=0 底座 … HEIGHT-1 顶端）：
 * - 方块实体与电网连接只挂在底座（PART=0）
 * - 碰撞体积 1×1×HEIGHT 完整（每格都是真实方块，无引擎"实体碰撞仅查询
 *   包围盒±1格内方块位置"导致的穿模问题）
 * - 点击任意一段都视为操作整根杆（界面/破坏均作用于底座）
 * - 破坏任意一段自动移除整根杆并清除电网连接
 * - 放置需要底座上方共 HEIGHT 格可放置空间
 */
public abstract class AbstractPoleBlock extends BaseEntityBlock {

    // 分段序号：0 = 底座（含方块实体），1~HEIGHT-1 = 上部杆体
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 4);
    // 整根杆高度（格）
    public static final int HEIGHT = 5;

    protected AbstractPoleBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(PART, 0));
    }

    /**
     * 该杆使用的方块实体类型（底座创建方块实体）
     */
    protected abstract BlockEntityType<?> blockEntityType();

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    /**
     * 底座位置（方块实体与电网连接所在）
     */
    public static BlockPos basePos(BlockState state, BlockPos pos) {
        return pos.below(state.getValue(PART));
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    // 放置与拆除

    @Override
    @Nullable
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        // 需要整根杆的可放置空间
        for (int i = 0; i < HEIGHT; i++) {
            BlockPos pos = context.getClickedPos().above(i);
            if (!context.getLevel().getBlockState(pos).canBeReplaced(context)) {
                return null;
            }
        }
        return this.defaultBlockState();
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state,
                            @Nullable LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()) {
            for (int i = 1; i < HEIGHT; i++) {
                level.setBlock(pos.above(i), this.defaultBlockState().setValue(PART, i), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction facing,
                                           @NotNull BlockState neighborState, @NotNull LevelAccessor level,
                                           @NotNull BlockPos pos, @NotNull BlockPos neighborPos) {
        // 上部杆体必须立在 PART-1 之上（下方被移除时自行塌落，剩余部分由 onRemove 清理）
        if (facing == Direction.DOWN && state.getValue(PART) > 0) {
            return neighborState.is(this) && neighborState.getValue(PART) == state.getValue(PART) - 1
                    ? state
                    : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, facing, neighborState, level, pos, neighborPos);
    }

    @Override
    public void playerWillDestroy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state,
                                  @NotNull Player player) {
        // 破坏上半段：改为破坏底座（掉落表只对底座生效，保证整根杆只掉落一次）
        if (!level.isClientSide() && state.getValue(PART) != 0) {
            BlockPos base = basePos(state, pos);
            if (level.getBlockState(base).is(this)) {
                level.destroyBlock(base, !player.isCreative());
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                         @NotNull BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            // 移除整根杆其余部分（递归进入时其余部分已是空气，自动终止）
            BlockPos base = basePos(state, pos);
            for (int i = 0; i < HEIGHT; i++) {
                BlockPos partPos = base.above(i);
                if (i != state.getValue(PART) && level.getBlockState(partPos).is(this)) {
                    level.removeBlock(partPos, false);
                }
            }
            // 清除所有电网连接（双向移除，方块实体在底座）
            BlockEntity be = level.getBlockEntity(base);
            if (be instanceof PowerLinkable linkable) {
                for (BlockPos conn : linkable.getConnections()) {
                    BlockEntity other = level.getBlockEntity(conn);
                    if (other instanceof PowerLinkable otherNode) {
                        otherNode.removeConnection(base);
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    // 交互

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                          @NotNull Player player, @NotNull InteractionHand hand,
                                          @NotNull BlockHitResult hit) {
        // 潜行右键：取消进行中的电力连接
        if (player.isShiftKeyDown() && !level.isClientSide()
                && PowerLinkHelper.cancelPendingLink(player)) {
            return InteractionResult.CONSUME;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // 右键：打开底座的电网界面（电力连接在界面内通过按键完成）
        BlockPos base = basePos(state, pos);
        BlockEntity be = level.getBlockEntity(base);
        if (be != null && be.getType() == blockEntityType() && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, (MenuProvider) be, base);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
