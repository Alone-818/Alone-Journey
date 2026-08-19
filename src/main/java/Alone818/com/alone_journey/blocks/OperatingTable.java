package Alone818.com.alone_journey.blocks;

import Alone818.com.alone_journey.blockentities.OperatingTableBlockEntity;
import Alone818.com.alone_journey.events.SurgeryEffectEvent;
import Alone818.com.alone_journey.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OperatingTable extends BaseEntityBlock {

    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 5);

    public OperatingTable() {
        super(BlockBehaviour.Properties.of()
                .strength(3.5F)
                .requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(@NotNull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new OperatingTableBlockEntity(pos, state);
    }

    /**
     * 使用延迟获取的方式，避免在类加载时提前调用 get()
     */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            @NotNull Level level,
            @NotNull BlockState state,
            @NotNull BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        if (type == ModBlockEntities.OPERATING_TABLE.get()) {
            return (BlockEntityTicker<T>) (BlockEntityTicker<OperatingTableBlockEntity>)
                    (Level lvl, BlockPos pos, BlockState st, OperatingTableBlockEntity be) ->
                            be.tick();
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
        if (!(be instanceof OperatingTableBlockEntity table)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);

        if (held.isEmpty()) {
            int maxSurgeryLevel = table.getMaxSurgeryLevelByTableLevel();
            int totalLevel = SurgeryEffectEvent.getTotalSurgeryLevel(player);
            int armorLevel = SurgeryEffectEvent.getSurgeryLevel(player, SurgeryEffectEvent.SURGERY_ARMOR);
            int damageLevel = SurgeryEffectEvent.getSurgeryLevel(player, SurgeryEffectEvent.SURGERY_DAMAGE);
            int speedLevel = SurgeryEffectEvent.getSurgeryLevel(player, SurgeryEffectEvent.SURGERY_SPEED);
            int healthLevel = SurgeryEffectEvent.getSurgeryLevel(player, SurgeryEffectEvent.SURGERY_HEALTH);
            int luckLevel = SurgeryEffectEvent.getSurgeryLevel(player, SurgeryEffectEvent.SURGERY_LUCK);
            player.sendSystemMessage(Component.literal(
                    "§7手术台：" + table.getOpLevel() + "级 | " +
                            "成功率：" + (int)(table.getSuccessRate() * 100) + "%"));
            player.sendSystemMessage(Component.literal(
                    "§7你的手术等级：总 " + totalLevel + "/" + maxSurgeryLevel +
                            " | 护甲 " + armorLevel + " | 伤害 " + damageLevel + " | 速度 " + speedLevel +
                            " | 生命 " + healthLevel + " | 幸运 " + luckLevel));
            return InteractionResult.SUCCESS;
        }

        // 1. 提交材料（锭类）直接提升等级
        if (table.canDepositMaterial(held)) {
            int before = table.getOpLevel();
            if (table.depositMaterial(held, player)) {
                player.sendSystemMessage(Component.literal(
                        "提交成功！手术等级： " + before + " → " + table.getOpLevel() + " 级"));
                level.setBlock(pos, state.setValue(LEVEL, table.getOpLevel()), 3);
            } else {
                player.sendSystemMessage(Component.literal("手术台已满级，无法继续提交材料。"));
            }
            return InteractionResult.SUCCESS;
        }

        // 2. 执行手术（剑类）概率提升玩家手术等级
        if (table.isSurgeryMaterial(held)) {
            table.performSurgery(held, player);
            return InteractionResult.SUCCESS;
        }

        player.sendSystemMessage(Component.literal("§e这不是有效的手术材料。"));
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
            if (be instanceof OperatingTableBlockEntity table) {
                for (int i = 0; i < table.getContainerSize(); i++) {
                    ItemStack stack = table.getItem(i);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
