package Alone818.com.alone_journey.blockentities;

import Alone818.com.alone_journey.init.ModBlockEntities;
import Alone818.com.alone_journey.menus.VoidMinerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 虚空采矿机方块实体
 *
 * 功能特性：
 * - 基于方块中心位置7×7范围（XZ平面），向上高5格的立方体区域
 * - 每40分钟为一个周期，对范围内的矿石进行一次采掘
 * - 每个矿石方块可被采掘3次，采掘3次后方块消失
 * - 每次采掘产出 基础产物×倍率 的矿物，存入27格机械仓库
 * - 支持右键打开机械仓库（原版箱子界面，27格库存）
 * - 用电设备：每次采掘消耗 400 FE，由电网（控制核心）供电
 */
public class VoidMinerBlockEntity extends BlockEntity implements Container, MenuProvider, PowerGridMachine {

    private static final int CONTAINER_SIZE = 27; // 27格仓库（与箱子一致）
    private static final int TICKS_PER_CYCLE = 200; // 40分钟一个采掘周期

    // 每个矿石方块最多被采掘的次数，采满后方块消失
    private static final int MAX_MINES_PER_ORE = 3;
    // 每次采掘的产出倍率（基础产物数量 × 该倍率）
    private static final int MINING_MULTIPLIER = 2;

    // 用电参数：每次采掘消耗 400 FE，内部储能 4,000 FE
    public static final int ENERGY_PER_CYCLE = 8000;
    private static final int ENERGY_CAPACITY = 16_000;
    private static final int MAX_RECEIVE = 1_000;

    // NBT 标签
    public static final String TAG_TICKS = "TicksElapsed";
    public static final String TAG_ENERGY = "Energy";
    public static final String TAG_PROGRESS = "MineProgress";
    public static final String TAG_PROGRESS_POS = "Pos";
    public static final String TAG_PROGRESS_COUNT = "Count";

    // 内部电力存储（只进不出，采掘时内部扣除）
    private final MinerEnergyStorage energy = new MinerEnergyStorage();
    private LazyOptional<IEnergyStorage> energyLazy = LazyOptional.of(() -> energy);

    // 电量客户端同步（工程师护目镜 HUD 依赖客户端电量，变化时节流广播）
    private static final int SYNC_INTERVAL = 20; // tick
    private int syncTimer = 0;
    private int lastSyncedEnergy = -1;

    private int ticksElapsed = 0;

    // 记录每个矿石方块已被采掘的次数（key 为方块位置的 asLong 值）
    private final Map<Long, Integer> mineProgress = new HashMap<>();

    // 存储矿石产出
    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);

    public VoidMinerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VOID_MINER.get(), pos, state);
    }


    public void tick() {
        if (level.isClientSide()) {
            return;
        }

        ticksElapsed++;

        // 每40分钟采掘一次（用电设备：电量不足时跳过本次，等待电网供电）
        if (ticksElapsed >= TICKS_PER_CYCLE) {
            ticksElapsed = 0;
            if (energy.getEnergyStored() >= ENERGY_PER_CYCLE) {
                energy.drainInternal(ENERGY_PER_CYCLE);
                processOres();
            }
        }

        // 电量变化时节流同步客户端（护目镜 HUD / 远程读取依赖）
        if (syncTimer++ >= SYNC_INTERVAL) {
            syncTimer = 0;
            if (energy.getEnergyStored() != lastSyncedEnergy) {
                lastSyncedEnergy = energy.getEnergyStored();
                syncToClient();
            }
        }
    }

    // PowerGridMachine 接口实现

    /**
     * 采矿机对核心等级无要求（0~5 级通用）
     */
    @Override
    public int getRequiredCoreLevel() {
        return 0;
    }

    /**
     * 处理周围的矿石
     * 每个矿石方块每周期被采掘一次，产出基础产物×倍率的矿物
     * 同一方块被采掘满3次后消失
     */
    private void processOres() {
        // 以方块中心为中心，7×7范围，向上高5格
        BlockPos center = worldPosition;
        int range = 3; // 3格半径 = 7×7范围
        int height = 5;

        // 记录本周期扫描到的矿石位置，用于清理过期进度
        Set<Long> foundOres = new HashSet<>();

        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                for (int y = 0; y <= height; y++) {
                    BlockPos pos = center.offset(x, y, z);

                    if (!level.isLoaded(pos)) {
                        continue;
                    }

                    BlockState blockState = level.getBlockState(pos);
                    ItemStack output = checkOreBlock(blockState);
                    if (output == null) {
                        continue;
                    }

                    foundOres.add(pos.asLong());

                    // 本次采掘产出：基础产物 × 倍率
                    addItemStack(output, output.getCount() * MINING_MULTIPLIER);

                    // 累计该方块的采掘次数
                    int mined = mineProgress.merge(pos.asLong(), 1, Integer::sum);
                    if (mined >= MAX_MINES_PER_ORE) {
                        // 采掘3次后矿石消失（不掉落方块，产物已存入仓库）
                        level.removeBlock(pos, false);
                        mineProgress.remove(pos.asLong());
                    }
                }
            }
        }

        // 清理已不存在的矿石的进度记录（如被玩家提前挖掉）
        mineProgress.keySet().removeIf(posLong -> !foundOres.contains(posLong));

        setChanged();
    }

    /**
     * 检查方块是否为矿石并返回单次采掘的基础产物
     * 硬编码检测常见矿石方块
     */
    private ItemStack checkOreBlock(BlockState blockState) {
        if (blockState.is(Blocks.IRON_ORE)) return new ItemStack(Items.IRON_INGOT);
        if (blockState.is(Blocks.DEEPSLATE_IRON_ORE)) return new ItemStack(Items.IRON_INGOT);
        if (blockState.is(Blocks.GOLD_ORE)) return new ItemStack(Items.GOLD_INGOT);
        if (blockState.is(Blocks.DEEPSLATE_GOLD_ORE)) return new ItemStack(Items.GOLD_INGOT);
        if (blockState.is(Blocks.DIAMOND_ORE)) return new ItemStack(Items.DIAMOND);
        if (blockState.is(Blocks.DEEPSLATE_DIAMOND_ORE)) return new ItemStack(Items.DIAMOND);
        if (blockState.is(Blocks.EMERALD_ORE)) return new ItemStack(Items.EMERALD);
        if (blockState.is(Blocks.DEEPSLATE_EMERALD_ORE)) return new ItemStack(Items.EMERALD);
        if (blockState.is(Blocks.REDSTONE_ORE)) return new ItemStack(Items.REDSTONE);
        if (blockState.is(Blocks.DEEPSLATE_REDSTONE_ORE)) return new ItemStack(Items.REDSTONE);
        if (blockState.is(Blocks.LAPIS_ORE)) return new ItemStack(Items.LAPIS_LAZULI);
        if (blockState.is(Blocks.DEEPSLATE_LAPIS_ORE)) return new ItemStack(Items.LAPIS_LAZULI);
        if (blockState.is(Blocks.COAL_ORE)) return new ItemStack(Items.COAL);
        if (blockState.is(Blocks.DEEPSLATE_COAL_ORE)) return new ItemStack(Items.COAL);
        if (blockState.is(Blocks.QUARTZ_BLOCK)) return new ItemStack(Items.QUARTZ);
        if (blockState.is(Blocks.ANCIENT_DEBRIS)) return new ItemStack(Items.NETHERITE_SCRAP);
        if (blockState.is(Blocks.COPPER_ORE)) return new ItemStack(Items.COPPER_INGOT);
        if (blockState.is(Blocks.DEEPSLATE_COPPER_ORE)) return new ItemStack(Items.COPPER_INGOT);

        return null;
    }

    /**
     * 添加物品到容器中
     * 自动拆分堆叠，超出一格最大堆叠数量时会占用多个格子
     */
    private void addItemStack(ItemStack item, int count) {
        if (count <= 0 || item.isEmpty()) {
            return;
        }

        ItemStack remaining = item.copy();
        remaining.setCount(count);

        for (int i = 0; i < items.size() && !remaining.isEmpty(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) {
                int toPlace = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                items.set(i, new ItemStack(remaining.getItem(), toPlace));
                remaining.shrink(toPlace);
            } else if (ItemStack.isSameItemSameTags(stack, remaining)) {
                int space = stack.getMaxStackSize() - stack.getCount();
                if (space > 0) {
                    int toPlace = Math.min(remaining.getCount(), space);
                    stack.grow(toPlace);
                    remaining.shrink(toPlace);
                }
            }
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(TAG_TICKS, ticksElapsed);
        tag.putInt(TAG_ENERGY, energy.getEnergyStored());
        ContainerHelper.saveAllItems(tag, items);

        // 保存矿石采掘进度
        ListTag progressList = new ListTag();
        for (Map.Entry<Long, Integer> entry : mineProgress.entrySet()) {
            CompoundTag progressTag = new CompoundTag();
            progressTag.putLong(TAG_PROGRESS_POS, entry.getKey());
            progressTag.putInt(TAG_PROGRESS_COUNT, entry.getValue());
            progressList.add(progressTag);
        }
        tag.put(TAG_PROGRESS, progressList);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        ticksElapsed = tag.getInt(TAG_TICKS);
        energy.setEnergy(tag.getInt(TAG_ENERGY));
        ContainerHelper.loadAllItems(tag, items);

        // 读取矿石采掘进度
        mineProgress.clear();
        ListTag progressList = tag.getList(TAG_PROGRESS, Tag.TAG_COMPOUND);
        for (int i = 0; i < progressList.size(); i++) {
            CompoundTag progressTag = progressList.getCompound(i);
            mineProgress.put(progressTag.getLong(TAG_PROGRESS_POS), progressTag.getInt(TAG_PROGRESS_COUNT));
        }
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
        return Component.translatable("block.alone_journey.void_miner");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new VoidMinerMenu(pContainerId, pPlayerInventory, this);
    }

    // 能力暴露（供控制核心向其供电）
    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyLazy.cast();
        }
        return super.getCapability(cap, side);
    }

    /**
     * 电量变化后广播方块更新，使客户端收到最新数据（护目镜 HUD 显示）
     */
    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // 方块实体数据同步（客户端护目镜 HUD 依赖电量数据）

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyLazy.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        energyLazy = LazyOptional.of(() -> energy);
    }

    /**
     * 采矿机内部电力存储：允许外部输入（电网供电），不允许外部输出
     */
    public static class MinerEnergyStorage extends EnergyStorage {

        public MinerEnergyStorage() {
            super(ENERGY_CAPACITY, MAX_RECEIVE, 0);
        }

        /**
         * 采掘耗电（内部扣除）
         */
        public void drainInternal(int amount) {
            this.energy = Math.max(0, this.energy - amount);
        }

        /**
         * 读档时直接设置当前电量
         */
        public void setEnergy(int value) {
            this.energy = Mth.clamp(value, 0, getMaxEnergyStored());
        }
    }
}
