package Alone818.com.alone_journey.blockentities;

import Alone818.com.alone_journey.init.ModBlockEntities;
import Alone818.com.alone_journey.menus.FuelGeneratorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 燃料发电机方块实体
 *
 * 功能特性：
 * - 消耗燃料（熔炉可燃物）产生FE电力
 * - 每份燃料的总发电量固定 = 燃烧时间(刻) × FE_PER_BURN_TICK
 *   例：煤炭1600刻 = 16,000 FE
 * - 有燃料时发电功率恒定：BASE_GENERATION × 燃烧速率 FE/t
 * - 燃烧速率决定发电时间：速率越高，燃料消耗越快，发电时间越短（总发电量不变）
 * - 内部可存储 100,000 FE 电力，可通过线缆/其他机器提取（最大输出 5,000 FE/t）
 * - 4个升级槽位（为机器升级系统预留，暂无效果）
 */
public class FuelGeneratorBlockEntity extends BlockEntity implements MenuProvider, PowerGridMachine {

    public static final int FUEL_SLOT = 0;
    public static final int TOTAL_SLOTS = 5; // 1个燃料槽 + 4个升级槽

    // 机器参数
    private static final int MAX_ENERGY = 100_000;  // 内部电力存储上限
    private static final int BASE_GENERATION = 80;  // 基础发电功率 FE/t（燃烧速率=1时）
    private static final int FE_PER_BURN_TICK = 10; // 每个熔炉燃烧刻对应的发电量 FE
    private static final int MAX_EXTRACT = 5_000;   // 对外最大输出 FE/t

    // NBT 标签
    public static final String TAG_ENERGY = "Energy";
    public static final String TAG_INVENTORY = "Inventory";
    public static final String TAG_BURN_REMAINING = "BurnRemaining";
    public static final String TAG_BURN_MAX = "BurnMax";

    // 内部电力存储（不允许外部输入，仅允许输出）
    private final MachineEnergyStorage energy = new MachineEnergyStorage(MAX_ENERGY, MAX_EXTRACT);

    // 内部物品存储（燃料槽 + 4个升级槽）
    private final ItemStackHandler inventory = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == FUEL_SLOT) {
                return ForgeHooks.getBurnTime(stack, null) > 0;
            }
            // 升级槽：为机器升级系统预留，目前不接受任何物品
            return isUpgradeItem(stack);
        }
    };

    // 当前燃料剩余可发电量 / 装填燃料时的总发电量（用于燃烧进度显示）
    private int burnRemaining = 0;
    private int burnMax = 0;

    // 电量客户端同步（工程师护目镜 HUD 依赖客户端电量，变化时节流广播）
    private static final int SYNC_INTERVAL = 20; // tick
    private int syncTimer = 0;
    private int lastSyncedEnergy = -1;

    private LazyOptional<IEnergyStorage> energyLazy = LazyOptional.of(() -> energy);
    private LazyOptional<IItemHandler> itemHandlerLazy = LazyOptional.of(() -> inventory);

    // 客户端同步数据（索引：0=当前电量 1=电量上限 2=剩余可发电量 3=燃料总发电量 4=燃烧速率 5=当前发电功率 6=是否燃烧中）
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energy.getEnergyStored();
                case 1 -> energy.getMaxEnergyStored();
                case 2 -> burnRemaining;
                case 3 -> burnMax;
                case 4 -> getBurnRate();
                case 5 -> getCurrentGeneration();
                case 6 -> isBurning() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // 客户端接收服务端同步数据时写入（服务端不会调用此方法）
            switch (index) {
                case 0 -> energy.setEnergyClient(value);
                case 2 -> burnRemaining = value;
                case 3 -> burnMax = value;
            }
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    public FuelGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUEL_GENERATOR.get(), pos, state);
    }

    // PowerGridMachine 接口实现

    /**
     * 发电机对核心等级无要求（0~5 级通用，任何控制核心都能调度它）
     */
    @Override
    public int getRequiredCoreLevel() {
        return 0;
    }

    public void tick() {
        if (level == null || level.isClientSide()) {
            return;
        }

        boolean changed = false;

        // 电量未满且当前无燃料燃烧时，尝试装填新燃料
        if (burnRemaining <= 0 && energy.getEnergyStored() < energy.getMaxEnergyStored()) {
            changed |= tryLoadFuel();
        }

        // 燃烧发电：发电功率恒定，同时以相同速率消耗燃料能量
        if (burnRemaining > 0) {
            int room = energy.getMaxEnergyStored() - energy.getEnergyStored();
            int amount = Math.min(getCurrentGeneration(), Math.min(burnRemaining, room));
            if (amount > 0) {
                energy.produceEnergy(amount);
                burnRemaining -= amount;
                changed = true;
            }
            // 电量已满时暂停燃烧（保留剩余燃料能量）
        }

        if (changed) {
            setChanged();
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

    /**
     * 尝试从燃料槽装填新燃料
     * 燃料的总发电量 = 熔炉燃烧时间(刻) × FE_PER_BURN_TICK
     */
    private boolean tryLoadFuel() {
        ItemStack fuel = inventory.getStackInSlot(FUEL_SLOT);
        if (fuel.isEmpty()) {
            return false;
        }

        int burnTime = ForgeHooks.getBurnTime(fuel, null);
        if (burnTime <= 0) {
            return false;
        }

        burnRemaining = burnTime * FE_PER_BURN_TICK;
        burnMax = burnRemaining;

        // 消耗燃料，处理有容器残留的情况（如熔岩桶）
        if (fuel.hasCraftingRemainingItem()) {
            inventory.setStackInSlot(FUEL_SLOT, fuel.getCraftingRemainingItem());
        } else {
            fuel.shrink(1);
        }

        return true;
    }

    /**
     * 当前燃烧速率（决定燃料消耗与发电的快慢，不影响总发电量）
     * TODO 机器升级系统：根据升级槽物品计算燃烧速率
     */
    public int getBurnRate() {
        return 1;
    }

    /**
     * 判断是否为机器升级物品
     * TODO 机器升级系统
     */
    public boolean isUpgradeItem(ItemStack stack) {
        return false;
    }

    /**
     * 当前发电功率（FE/t），有燃料燃烧时按此功率恒定发电
     */
    public int getCurrentGeneration() {
        return BASE_GENERATION * getBurnRate();
    }

    /**
     * 是否正在燃烧发电
     */
    public boolean isBurning() {
        return burnRemaining > 0;
    }

    /**
     * 破坏方块时掉落内部物品
     */
    public void dropInventory(Level level, BlockPos pos) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    public ItemStackHandler getItemHandler() {
        return inventory;
    }

    public MachineEnergyStorage getEnergyStorage() {
        return energy;
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
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        // 注意：EnergyStorage 序列化产物是 IntTag 而非 CompoundTag，
        // 必须用 putInt/getInt 存取，getCompound 读不回来
        tag.putInt(TAG_ENERGY, energy.getEnergyStored());
        tag.put(TAG_INVENTORY, inventory.serializeNBT());
        tag.putInt(TAG_BURN_REMAINING, burnRemaining);
        tag.putInt(TAG_BURN_MAX, burnMax);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains(TAG_ENERGY)) {
            energy.setEnergyClient(tag.getInt(TAG_ENERGY));
        }
        if (tag.contains(TAG_INVENTORY)) {
            inventory.deserializeNBT(tag.getCompound(TAG_INVENTORY));
        }
        burnRemaining = tag.getInt(TAG_BURN_REMAINING);
        burnMax = tag.getInt(TAG_BURN_MAX);
    }

    // 能量与物品能力暴露（供线缆/漏斗等外部交互）
    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyLazy.cast();
        }
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandlerLazy.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyLazy.invalidate();
        itemHandlerLazy.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        energyLazy = LazyOptional.of(() -> energy);
        itemHandlerLazy = LazyOptional.of(() -> inventory);
    }

    // MenuProvider 接口实现
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.alone_journey.fuel_generator");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FuelGeneratorMenu(containerId, playerInventory, worldPosition);
    }

    /**
     * 发电机内部电力存储：不允许外部输入，仅允许输出
     */
    public static class MachineEnergyStorage extends EnergyStorage {

        public MachineEnergyStorage(int capacity, int maxExtract) {
            super(capacity, 0, maxExtract);
        }

        /**
         * 机器内部发电写入
         */
        public int produceEnergy(int amount) {
            int received = Math.min(amount, getMaxEnergyStored() - this.energy);
            if (received > 0) {
                this.energy += received;
            }
            return received;
        }

        /**
         * 客户端同步用：直接设置当前电量
         */
        public void setEnergyClient(int value) {
            this.energy = Mth.clamp(value, 0, getMaxEnergyStored());
        }
    }
}
