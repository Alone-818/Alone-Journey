package Alone818.com.alone_journey.blockentities;

import Alone818.com.alone_journey.events.SurgeryEffectEvent;
import Alone818.com.alone_journey.init.ModBlockEntities;
import Alone818.com.alone_journey.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * 手术台方块实体
 * 存储手术等级、材料槽位以及手术逻辑
 *
 * 手术台等级决定玩家能进行的手术等级上限：
 * 等级1→玩家手术上限4级
 * 等级2→玩家手术上限8级
 * 等级3→玩家手术上限12级
 * 等级4→玩家手术上限16级
 * 等级5→玩家手术上限21级
 *
 * 手术材料：
 * 1. 钻石胸甲: 提升护甲术（护甲+2，韧性+1）
 * 2. 钻石剑: 提升伤害术（造成伤害+5%）
 * 3. 末影水晶: 提升速度术（移动速度+5%，跳跃高度+5%）
 *
 * 首次手术后玩家获得对失明、剧毒、反胃、黑暗效果的免疫
 */
public class OperatingTableBlockEntity extends BlockEntity implements Container {

    private static final int MAX_TABLE_LEVEL = 5; // 手术台最大等级（0-5共6个状态，实际等级5为满级）
    private static final double BASE_SUCCESS_RATE = 0.7;

    private int level = 0;

    // 新增：材料槽位（共3个槽位）
    private final NonNullList<ItemStack> materials = NonNullList.withSize(3, ItemStack.EMPTY);
    private final Random random = new Random();

    // NBT 标签
    public static final String TAG_LEVEL = "Level";
    public static final String TAG_MATERIALS = "Materials";

    public OperatingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OPERATING_TABLE.get(), pos, state);
    }

    public int getOpLevel() {
        return level;
    }

    public int getMaxLevel() {
        return MAX_TABLE_LEVEL;
    }

    public int getMaxSurgeryLevelByTableLevel() {
        return SurgeryEffectEvent.getMaxLevelByTableLevel(level);
    }

    public double getSuccessRate() {
        // 随等级提升，成功率略有提升
        if (level <= 0) return BASE_SUCCESS_RATE;
        return BASE_SUCCESS_RATE + (level - 1) * 0.07;
    }

    /**
     * 检查是否为手术材料
     * 包括：铁锭(护甲术)、烈焰棒(伤害术)、末影水晶(速度术)、金苹果(生命术)、紫水晶碎片(幸运术)
     */
    public boolean isSurgeryMaterial(ItemStack stack) {
        return stack.is(Items.IRON_INGOT)
                || stack.is(Items.BLAZE_ROD)
                || stack.is(Items.END_CRYSTAL)
                || stack.is(Items.GOLDEN_APPLE)
                || stack.is(Items.AMETHYST_SHARD);
    }


    /**
     * 执行手术
     *
     * @param item 手术材料：钻石胸甲、钻石剑、末影水晶
     * @param player 进行手术的玩家
     * @return 手术是否成功
     */
    public boolean performSurgery(ItemStack item, Player player) {
        // 获取手术类型（**必须在消耗材料之前获取类型**，否则钻石胸甲/剑会被缩减为0后无法识别）
        int surgeryType = SurgeryEffectEvent.getSurgeryTypeFromMaterial(item);
        if (surgeryType < 0) {
            return false;
        }

        // 检查玩家总手术等级是否已满级
        int currentTotalLevel = SurgeryEffectEvent.getTotalSurgeryLevel(player);
        int maxLevel = SurgeryEffectEvent.getMaxLevelByTableLevel(level);

        if (currentTotalLevel >= maxLevel) {
            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("§c手术等级已达最高！当前总等级：" + currentTotalLevel + "/" + maxLevel + "，请勿再次提交手术材料"));
            return false;
        }

        // 消耗材料（无论成功与否都消耗）
        if (!item.isEmpty()) {
            item.shrink(8);
        }

        // 手术后损失4点生命值
        if (!player.isInvulnerableTo(player.level().damageSources().generic())) {
            player.hurt(player.level().damageSources().generic(), 4.0F);
        }

        // 随机判定成功
        boolean success = random.nextDouble() < getSuccessRate();

        if (success) {
            // 成功：应用手术效果
            boolean applied = SurgeryEffectEvent.applySurgery(player, surgeryType, level);

            if (applied) {
                player.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal("§a手术成功！" +
                                SurgeryEffectEvent.getSurgeryTypeName(surgeryType) + "提升 1 级（总等级：" +
                                (currentTotalLevel + 1) + "/" + maxLevel + "）"));
                return true;
            }
        }

        player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal("§c手术失败！材料已消耗，等级保持在 " + currentTotalLevel + " 级"));
        return false;
    }

    /**
     * 提交材料，按等级消耗对应的金属
     *
     * @return 是否成功提升等级
     */
    public boolean depositMaterial(ItemStack stack, Player player) {
        if (level >= MAX_TABLE_LEVEL) {
            return false;
        }

        if (!isRequiredMaterial(stack)) {
            return false;
        }

        int requiredCount = getRequiredCount();
        if (stack.getCount() < requiredCount) {
            return false;
        }

        // 消耗材料
        stack.shrink(requiredCount);

        // 提升等级
        this.level++;
        setChanged();
        return true;
    }

    /**
     * 检查玩家是否可以对指定类型的手术进行提升
     */
    public boolean canUpgradeSurgery(Player player, int surgeryType) {
        int currentTotalLevel = SurgeryEffectEvent.getTotalSurgeryLevel(player);
        int maxLevel = SurgeryEffectEvent.getMaxLevelByTableLevel(level);
        return currentTotalLevel < maxLevel;
    }

    /**
     * 检查是否可以提交材料（用于检查物品是否匹配当前等级需要的材料）
     */
    public boolean canDepositMaterial(ItemStack stack) {
        return isRequiredMaterial(stack);
    }

    /**
     * 获取当前等级所需的材料类型
     * 0级→1级: 铜锭, 1级→2级: 铁锭, 2级→3级: 金锭, 3级→4级: 钻石锭, 4级→5级: 下界合金锭
     * 手术台有0-4共5个等级，每个等级都需要对应稀有度的矿锭
     */
    private boolean isRequiredMaterial(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // 每个等级都对应一种矿锭
        if (level == 0) return stack.is(net.minecraft.world.item.Items.COPPER_INGOT);
        if (level == 1) return stack.is(net.minecraft.world.item.Items.IRON_INGOT);
        if (level == 2) return stack.is(net.minecraft.world.item.Items.GOLD_INGOT);
        if (level == 3) return stack.is(net.minecraft.world.item.Items.DIAMOND);
        if (level == 4) return stack.is(net.minecraft.world.item.Items.NETHERITE_INGOT);
        return false;
    }

    /**
     * 获取当前等级需要的材料数量（固定16个）
     */
    public int getRequiredCount() {
        return 16;
    }

    /**
     * 获取当前等级的材料名称描述
     */
    public String getMaterialName() {
        switch (level) {
            case 0:
                return "铜锭";
            case 1:
                return "铁锭";
            case 2:
                return "金锭";
            case 3:
                return "钻石锭";
            case 4:
                return "下界合金锭";
            default:
                return "未知材料";
        }
    }

    @Override
    protected void saveAdditional(@NotNull net.minecraft.nbt.CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(TAG_LEVEL, level);
        ContainerHelper.saveAllItems(tag, materials);
    }

    @Override
    public void load(@NotNull net.minecraft.nbt.CompoundTag tag) {
        super.load(tag);
        level = tag.getInt(TAG_LEVEL);
        ContainerHelper.loadAllItems(tag, materials);
    }

    @Override
    public int getContainerSize() {
        return 3;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : materials) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int pSlot) {
        if (pSlot >= 0 && pSlot < materials.size()) {
            return materials.get(pSlot);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int pSlot, int pAmount) {
        if (pSlot >= 0 && pSlot < materials.size()) {
            ItemStack stack = materials.get(pSlot);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack removed = new ItemStack(stack.getItem(), Math.min(pAmount, stack.getCount()));
            stack.shrink(removed.getCount());
            return removed;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int pSlot) {
        if (pSlot >= 0 && pSlot < materials.size()) {
            ItemStack stack = materials.get(pSlot);
            materials.set(pSlot, ItemStack.EMPTY);
            return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int pSlot, ItemStack pStack) {
        if (pSlot >= 0 && pSlot < materials.size()) {
            materials.set(pSlot, pStack);
        }
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return pPlayer.distanceToSqr(worldPosition.getX() + 0.5, pPlayer.getY(), worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {

    }

    public void tick() {
    }
}