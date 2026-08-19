package Alone818.com.alone_journey.blockentities;

import Alone818.com.alone_journey.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 虚空采矿机方块实体
 *
 * 功能特性：
 * - 基于方块中心位置7×7范围（XZ平面），向上高5格的立方体区域
 * - 每10分钟为一个周期，自动收集周围带有ores标签的方块
 * - 产出一次对应的矿石 + 粗矿（产量为矿石数量×5）
 * - 支持右键打开机械仓库查看内容（参考箱子结构）
 * - 不需要能源，自动工作
 */
public class VoidMinerBlockEntity extends BlockEntity implements Container, MenuProvider {

    private static final int CONTAINER_SIZE = 27; // 3×3×3 的存储格
    private static final int TICKS_PER_CYCLE = 48000; //

    private int ticksElapsed = 0;

    // NBT 标签
    public static final String TAG_TICKS = "TicksElapsed";

    // 存储矿石与对应粗矿的映射
    private static final Map<ItemStack, ItemStack> ORE_TO_ROUGH_MAP = new HashMap<>();

    static {
        // 初始化矿石到粗矿的映射
        ORE_TO_ROUGH_MAP.put(new ItemStack(Items.IRON_ORE), new ItemStack(Items.RAW_IRON));
        ORE_TO_ROUGH_MAP.put(new ItemStack(Items.STONE), new ItemStack(Items.COBBLESTONE));
        ORE_TO_ROUGH_MAP.put(new ItemStack(Items.DEEPSLATE_IRON_ORE), new ItemStack(Items.RAW_IRON));
        ORE_TO_ROUGH_MAP.put(new ItemStack(Items.GOLD_ORE), new ItemStack(Items.RAW_GOLD));
        ORE_TO_ROUGH_MAP.put(new ItemStack(Items.DEEPSLATE_GOLD_ORE), new ItemStack(Items.RAW_GOLD));
        ORE_TO_ROUGH_MAP.put(new ItemStack(Items.DIAMOND_ORE), new ItemStack(Items.DIAMOND));
        ORE_TO_ROUGH_MAP.put(new ItemStack(Items.DEEPSLATE_DIAMOND_ORE), new ItemStack(Items.DIAMOND));
        ORE_TO_ROUGH_MAP.put(new ItemStack(Items.EMERALD_ORE), new ItemStack(Items.EMERALD));
        ORE_TO_ROUGH_MAP.put(new ItemStack(Items.DEEPSLATE_EMERALD_ORE), new ItemStack(Items.EMERALD));
        ORE_TO_ROUGH_MAP.put(new ItemStack(Items.REDSTONE_ORE), new ItemStack(Items.RAW_IRON));
        ORE_TO_ROUGH_MAP.put(new ItemStack(Items.DEEPSLATE_REDSTONE_ORE), new ItemStack(Items.RAW_IRON));
        ORE_TO_ROUGH_MAP.put(new ItemStack(Items.LAPIS_ORE), new ItemStack(Items.RAW_COPPER));
        ORE_TO_ROUGH_MAP.put(new ItemStack(Items.DEEPSLATE_LAPIS_ORE), new ItemStack(Items.RAW_COPPER));
        ORE_TO_ROUGH_MAP.put(new ItemStack(Items.COAL_ORE), new ItemStack(Items.COAL));
        ORE_TO_ROUGH_MAP.put(new ItemStack(Items.DEEPSLATE_COAL_ORE), new ItemStack(Items.COAL));
    }

    // 存储矿石产出数量
    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);

    public VoidMinerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VOID_MINER.get(), pos, state);
    }


    public void tick() {
        if (level.isClientSide()) {
            return;
        }

        ticksElapsed++;

        // 每10分钟检查一次
        if (ticksElapsed >= TICKS_PER_CYCLE) {
            processOres();
            ticksElapsed = 0;
        }
    }

    /**
     * 处理周围的矿石
     */
    private void processOres() {
        // 以方块中心为中心，7×7范围，向上高5格
        BlockPos center = worldPosition;
        int range = 3; // 3格半径 = 7×7范围
        int height = 5;

        Map<ItemStack, Integer> oreCounts = new HashMap<>();

        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                for (int y = 0; y <= height; y++) {
                    BlockPos pos = center.offset(x, y, z);

                    if (!level.isLoaded(pos)) {
                        continue;
                    }

                    BlockState blockState = level.getBlockState(pos);
                    ItemStack result = checkOreBlock(blockState);
                    if (result != null) {
                        oreCounts.merge(result, 1, Integer::sum);
                        // 不破坏方块，仅收集产物（保留矿石作为可再生矿物源）
                    }
                }
            }
        }

        // 处理收获
        for (Map.Entry<ItemStack, Integer> entry : oreCounts.entrySet()) {
            int count = entry.getValue();
            ItemStack oreStack = entry.getKey();

            // 获取对应的粗矿
            ItemStack roughStack = ORE_TO_ROUGH_MAP.get(oreStack);
            if (roughStack == null) {
                roughStack = oreStack;
            }

            // 产出矿石和粗矿
            // 矿石数量 + 粗矿数量×5
            addItemStack(oreStack, count);
            addItemStack(roughStack, count * 4);
        }

        setChanged();
    }

    /**
     * 检查方块是否为矿石并返回其物品
     * 硬编码检测常见矿石方块
     */
    private ItemStack checkOreBlock(BlockState blockState) {
        if (blockState.is(Blocks.IRON_ORE)) return new ItemStack(Items.IRON_ORE);
        if (blockState.is(Blocks.DEEPSLATE_IRON_ORE)) return new ItemStack(Items.IRON_NUGGET);
        if (blockState.is(Blocks.GOLD_ORE)) return new ItemStack(Items.GOLD_INGOT);
        if (blockState.is(Blocks.DEEPSLATE_GOLD_ORE)) return new ItemStack(Items.GOLD_NUGGET);
        if (blockState.is(Blocks.DIAMOND_ORE)) return new ItemStack(Items.DIAMOND);
        if (blockState.is(Blocks.DEEPSLATE_DIAMOND_ORE)) return new ItemStack(Items.DIAMOND);
        if (blockState.is(Blocks.EMERALD_ORE)) return new ItemStack(Items.EMERALD);
        if (blockState.is(Blocks.DEEPSLATE_EMERALD_ORE)) return new ItemStack(Items.EMERALD);
        if (blockState.is(Blocks.REDSTONE_ORE)) return new ItemStack(Items.RAW_IRON);
        if (blockState.is(Blocks.DEEPSLATE_REDSTONE_ORE)) return new ItemStack(Items.RAW_IRON);
        if (blockState.is(Blocks.LAPIS_ORE)) return new ItemStack(Items.RAW_COPPER);
        if (blockState.is(Blocks.DEEPSLATE_LAPIS_ORE)) return new ItemStack(Items.RAW_COPPER);
        if (blockState.is(Blocks.COAL_ORE)) return new ItemStack(Items.COAL);
        if (blockState.is(Blocks.DEEPSLATE_COAL_ORE)) return new ItemStack(Items.COAL);
        if (blockState.is(Blocks.QUARTZ_BLOCK)) return new ItemStack(Items.QUARTZ);
        if (blockState.is(Blocks.ANCIENT_DEBRIS)) return new ItemStack(Items.NETHERITE_SCRAP);
        if (blockState.is(Blocks.COPPER_ORE)) return new ItemStack(Items.RAW_COPPER);
        if (blockState.is(Blocks.DEEPSLATE_COPPER_ORE)) return new ItemStack(Items.RAW_COPPER);

        return null;
    }

    /**
     * 添加物品到容器中
     */
    private void addItemStack(ItemStack item, int count) {
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) {
                ItemStack newStack = new ItemStack(item.getItem(), count);
                items.set(i, newStack);
                return;
            } else if (stack.getItem() == item.getItem() && stack.getCount() + count <= stack.getMaxStackSize()) {
                items.set(i, new ItemStack(item.getItem(), stack.getCount() + count));
                return;
            }
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(TAG_TICKS, ticksElapsed);
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        ticksElapsed = tag.getInt(TAG_TICKS);
        ContainerHelper.loadAllItems(tag, items);
    }

    // Container 接口实现
    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int pSlot) {
        if (pSlot >= 0 && pSlot < items.size()) {
            return items.get(pSlot);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int pSlot, int pAmount) {
        if (pSlot >= 0 && pSlot < items.size()) {
            ItemStack stack = items.get(pSlot);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            int amount = Math.min(pAmount, stack.getCount());
            ItemStack removed = new ItemStack(stack.getItem(), amount);
            stack.shrink(amount);
            return removed;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int pSlot) {
        if (pSlot >= 0 && pSlot < items.size()) {
            ItemStack stack = items.get(pSlot);
            items.set(pSlot, ItemStack.EMPTY);
            return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int pSlot, ItemStack pStack) {
        if (pSlot >= 0 && pSlot < items.size()) {
            items.set(pSlot, pStack);
        }
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return pPlayer.distanceToSqr(this.worldPosition.getX() + 0.5, pPlayer.getY(), this.worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    // MenuProvider 接口实现
    @Override
    public Component getDisplayName() {
        return Component.literal("虚空采矿机");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return null;
    }
}