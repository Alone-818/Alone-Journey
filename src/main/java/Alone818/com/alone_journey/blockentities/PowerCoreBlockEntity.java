package Alone818.com.alone_journey.blockentities;

import Alone818.com.alone_journey.Items.core_upgrade;
import Alone818.com.alone_journey.init.ModBlockEntities;
import Alone818.com.alone_journey.menus.PowerCoreMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 控制核心方块实体
 *
 * 功能特性：
 * - 创建、控制、管理一个电力网络
 * - 与信号杆通过电力连接组成电网（潜行右键拉线，距离≤40格）
 * - 每个电网节点（控制核心/信号杆）周围 5×5×5 区域为电力区域
 * - 每10tick一个周期：从电力区域内的发电设备抽取电力，向用电设备输送电力
 *   （机器声明所需核心等级，等级不足的机器不参与调度，见 PowerGridMachine）
 * - 核心等级 0~5：放入核心升级组件立即升级，网络储能 = 基础容量 ×（等级+1）
 * - 右键（非潜行）打开电网界面
 */
public class PowerCoreBlockEntity extends BlockEntity implements MenuProvider, PowerLinkable {

    // 电网参数
    public static final int NETWORK_CAPACITY = 1_000_000; // 基础网络储能（1级=等级0），实际容量 = 基础 ×（等级+1）
    public static final int MAX_CORE_LEVEL = 5;            // 核心最高等级
    public static final int TRANSFER_PER_MACHINE = 1_000; // 每台机器每周期最大传输量
    private static final int TRANSFER_INTERVAL = 10;      // 传输周期（tick）
    private static final int MAX_NETWORK_NODES = 64;      // 电网最大节点数（核心+信号杆）
    private static final int POWER_RADIUS = 2;            // 电力区域半径（5×5×5）

    // NBT 标签
    public static final String TAG_ENERGY = "Energy";
    public static final String TAG_CONNECTIONS = "Connections";
    public static final String TAG_CORE_LEVEL = "CoreLevel";
    public static final String TAG_UPGRADE_INVENTORY = "UpgradeInventory";

    // 网络储能（不允许外部直接输入输出，由核心内部统一调度）
    private final NetworkEnergyStorage energy = new NetworkEnergyStorage(NETWORK_CAPACITY);

    // 能量能力暴露（只读展示用：护目镜 HUD 读取电量；
    // NetworkEnergyStorage 的 maxReceive/maxExtract 均为 0，外部无法实际读写）
    private LazyOptional<IEnergyStorage> energyLazy = LazyOptional.of(() -> energy);

    // 电力连接的节点位置列表
    private final List<BlockPos> connections = new ArrayList<>();

    // 核心等级（0~5），决定网络储能上限与可驱动的机器
    private int coreLevel = 0;

    // 核心升级物品槽：0号 = 核心升级组件（放入立即消耗，等级+1）；1~3号 = 专属核心升级插件
    public static final int UPGRADE_SLOT = 0;
    public static final int PLUGIN_SLOT_START = 1;
    public static final int PLUGIN_SLOT_END = 3;

    private final ItemStackHandler upgradeInventory = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            // 仅0号槽：放入升级组件立即消耗并升级（deserializeNBT 不会触发此方法，读档安全）
            if (slot == UPGRADE_SLOT) {
                ItemStack stack = getStackInSlot(slot);
                if (!stack.isEmpty() && coreLevel < MAX_CORE_LEVEL) {
                    stack.shrink(1);
                    setCoreLevel(coreLevel + 1);
                }
            }
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == UPGRADE_SLOT) {
                // 满级后不再接受升级物品
                return coreLevel < MAX_CORE_LEVEL && isUpgradeItem(stack);
            }
            // 专属核心升级插件槽：为插件系统预留，目前不接受任何物品
            return isPluginItem(stack);
        }
    };

    private int tickCounter = 0;

    // 吞吐统计：每秒采样输入/输出增量，环形缓冲10分钟
    // 即时 = 最近1个采样；5分钟/10分钟 = 最近窗口的每秒平均
    public static final int RATE_MODE_INSTANT = 0;   // 即时（最近1秒）
    public static final int RATE_MODE_5MIN = 1;      // 5分钟平均
    public static final int RATE_MODE_10MIN = 2;     // 10分钟平均
    private static final int RATE_SAMPLE_SECONDS = 600; // 10分钟 @ 每秒1个采样
    private final long[] inSamples = new long[RATE_SAMPLE_SECONDS];
    private final long[] outSamples = new long[RATE_SAMPLE_SECONDS];
    private int sampleIndex = 0;   // 环形缓冲下一个写入位置
    private int sampleCount = 0;   // 已采样总数（封顶600）
    private long pendingIn = 0;    // 当前采样周期累计输入（FE）
    private long pendingOut = 0;   // 当前采样周期累计输出（FE）
    // 界面同步的吞吐缓存（FE/s）
    private int cachedInInstant = 0, cachedOutInstant = 0;
    private int cachedIn5Min = 0, cachedOut5Min = 0;
    private int cachedIn10Min = 0, cachedOut10Min = 0;

    // 有电状态（>0）上次同步值：跨越0边界时广播，客户端据此渲染电线颜色
    private boolean lastSyncedPowered = false;
    // 电量上次同步值与节流计时：变化时节流广播（护目镜 HUD 显示依赖客户端电量）
    private int lastSyncedEnergy = -1;
    private int energySyncTimer = 0;

    // 电网统计缓存（服务端周期刷新，避免界面同步每tick做BFS遍历）
    private static final int STATS_INTERVAL = 20;
    private int statsTimer = 0;
    private int cachedPoleCount = 0;
    private int cachedMachineCount = 0;

    // 电网机器缓存（调度直接使用，随统计一起周期刷新，避免每10tick全量扫描）
    private final Set<BlockPos> cachedMachines = new LinkedHashSet<>();
    private boolean networkDirty = true;

    // 界面同步数据
    // 索引：0=当前电量 1=储能上限 2=信号杆数 3=网络机器数 4=核心等级
    //      5=即时输入 6=即时输出 7=5分钟输入 8=5分钟输出 9=10分钟输入 10=10分钟输出（FE/s）
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energy.getEnergyStored();
                case 1 -> energy.getMaxEnergyStored();
                case 2 -> cachedPoleCount;
                case 3 -> cachedMachineCount;
                case 4 -> coreLevel;
                case 5 -> cachedInInstant;
                case 6 -> cachedOutInstant;
                case 7 -> cachedIn5Min;
                case 8 -> cachedOut5Min;
                case 9 -> cachedIn10Min;
                case 10 -> cachedOut10Min;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // 客户端接收服务端同步数据时写入（服务端不会调用此方法）
            switch (index) {
                // 电量先于容量到达，用不钲位的写入避免被旧的容量上限截断
                case 0 -> energy.setEnergyClient(value);
                case 1 -> energy.setCapacity(value);
                case 2 -> cachedPoleCount = value;
                case 3 -> cachedMachineCount = value;
                case 4 -> {
                    coreLevel = Mth.clamp(value, 0, MAX_CORE_LEVEL);
                }
                // 吞吐缓存（界面经同一份数据读取，必须回写）
                case 5 -> cachedInInstant = value;
                case 6 -> cachedOutInstant = value;
                case 7 -> cachedIn5Min = value;
                case 8 -> cachedOut5Min = value;
                case 9 -> cachedIn10Min = value;
                case 10 -> cachedOut10Min = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 11;
        }
    };

    public PowerCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POWER_CORE.get(), pos, state);
    }

    public void tick() {
        if (level == null || level.isClientSide()) {
            return;
        }

        // 每秒刷新一次电网缓存（节点、机器、统计），首次tick立即刷新；吞吐采样同步进行
        if (networkDirty || statsTimer++ >= STATS_INTERVAL) {
            statsTimer = 0;
            refreshNetworkCache();
            sampleThroughput();
        }

        tickCounter++;
        if (tickCounter < TRANSFER_INTERVAL) {
            return;
        }
        tickCounter = 0;

        transferEnergy();

        // 有电状态跨越0边界时同步客户端（电线颜色渲染依赖）
        boolean powered = energy.getEnergyStored() > 0;
        if (powered != lastSyncedPowered) {
            lastSyncedPowered = powered;
            syncToClient();
        }

        // 电量变化时节流同步客户端（护目镜 HUD 显示依赖），每秒最多广播一次
        if (energySyncTimer++ >= STATS_INTERVAL) {
            energySyncTimer = 0;
            if (energy.getEnergyStored() != lastSyncedEnergy) {
                lastSyncedEnergy = energy.getEnergyStored();
                syncToClient();
            }
        }
    }

    /**
     * 每秒采样一次吞吐并刷新界面缓存（在 tick 中调用）
     */
    private void sampleThroughput() {
        inSamples[sampleIndex] = pendingIn;
        outSamples[sampleIndex] = pendingOut;
        pendingIn = 0;
        pendingOut = 0;
        sampleIndex = (sampleIndex + 1) % RATE_SAMPLE_SECONDS;
        sampleCount = Math.min(sampleCount + 1, RATE_SAMPLE_SECONDS);

        cachedInInstant = toRate(inSamples[(sampleIndex + RATE_SAMPLE_SECONDS - 1) % RATE_SAMPLE_SECONDS]);
        cachedOutInstant = toRate(outSamples[(sampleIndex + RATE_SAMPLE_SECONDS - 1) % RATE_SAMPLE_SECONDS]);
        int count5 = Math.min(sampleCount, 300);
        int count10 = sampleCount;
        cachedIn5Min = toRate(windowAverage(inSamples, count5));
        cachedOut5Min = toRate(windowAverage(outSamples, count5));
        cachedIn10Min = toRate(windowAverage(inSamples, count10));
        cachedOut10Min = toRate(windowAverage(outSamples, count10));
    }

    /**
     * 最近 count 个采样的平均值（FE/s）
     */
    private long windowAverage(long[] samples, int count) {
        if (count <= 0) {
            return 0;
        }
        long sum = 0;
        for (int i = 1; i <= count; i++) {
            sum += samples[(sampleIndex + RATE_SAMPLE_SECONDS - i) % RATE_SAMPLE_SECONDS];
        }
        return sum / count;
    }

    /**
     * 采样值 -> 每秒速率（FE/s，安全收敛到 int）
     */
    private static int toRate(long samplePerSecond) {
        return (int) Math.min(samplePerSecond, Integer.MAX_VALUE);
    }

    /**
     * 吞吐速率读取（供界面与电网节点显示，单位 FE/s）
     *
     * @param mode RATE_MODE_INSTANT / RATE_MODE_5MIN / RATE_MODE_10MIN
     */
    public int getRateIn(int mode) {
        return switch (mode) {
            case RATE_MODE_5MIN -> cachedIn5Min;
            case RATE_MODE_10MIN -> cachedIn10Min;
            default -> cachedInInstant;
        };
    }

    /**
     * 吞吐速率读取（输出侧，单位 FE/s）
     */
    public int getRateOut(int mode) {
        return switch (mode) {
            case RATE_MODE_5MIN -> cachedOut5Min;
            case RATE_MODE_10MIN -> cachedOut10Min;
            default -> cachedOutInstant;
        };
    }

    /**
     * 刷新电网缓存：BFS收集节点，按各节点供电半径收集机器
     */
    private void refreshNetworkCache() {
        networkDirty = false;

        List<BlockPos> nodes = collectNetworkNodes();
        Set<BlockPos> machines = collectPoweredMachines(nodes);

        cachedPoleCount = nodes.size() - 1;
        cachedMachineCount = machines.size();

        cachedMachines.clear();
        cachedMachines.addAll(machines);
    }

    /**
     * 电网能量调度：使用缓存的机器集合（每秒刷新），
     * 从发电设备抽电，向用电设备送电
     */
    private void transferEnergy() {
        boolean changed = false;

        for (BlockPos pos : cachedMachines) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null) {
                // 机器已被破坏，等下次缓存刷新移除
                continue;
            }
            // 核心等级不足的机器不参与电网调度
            if (be instanceof PowerGridMachine machine && machine.getRequiredCoreLevel() > coreLevel) {
                continue;
            }
            IEnergyStorage storage = be.getCapability(ForgeCapabilities.ENERGY).orElse(null);
            if (storage == null) {
                continue;
            }

            // 从发电设备抽取电力（如燃料发电机）
            if (storage.canExtract()) {
                int room = energy.getMaxEnergyStored() - energy.getEnergyStored();
                int toPull = Math.min(TRANSFER_PER_MACHINE, room);
                int extracted = storage.extractEnergy(toPull, false);
                if (extracted > 0) {
                    energy.produceEnergy(extracted);
                    pendingIn += extracted;
                    changed = true;
                }
            }

            // 向用电设备输送电力
            if (storage.canReceive()) {
                int toPush = Math.min(TRANSFER_PER_MACHINE, energy.getEnergyStored());
                int pushed = storage.receiveEnergy(toPush, false);
                if (pushed > 0) {
                    energy.drainEnergy(pushed);
                    pendingOut += pushed;
                    changed = true;
                }
            }
        }

        if (changed) {
            setChanged();
        }
    }

    /**
     * 收集电网所有节点：从本核心出发，沿电力连接BFS遍历（含核心自身与所有连通的信号杆）
     */
    private List<BlockPos> collectNetworkNodes() {
        List<BlockPos> nodes = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        queue.add(worldPosition);
        visited.add(worldPosition);

        while (!queue.isEmpty() && nodes.size() < MAX_NETWORK_NODES) {
            BlockPos pos = queue.poll();
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof PowerLinkable linkable)) {
                // 连接失效（方块被破坏/替换），跳过
                continue;
            }

            nodes.add(pos);
            for (BlockPos conn : linkable.getConnections()) {
                if (visited.add(conn)) {
                    queue.add(conn);
                }
            }
        }

        return nodes;
    }

    /**
     * 收集电力区域内的所有能量设备（按各节点供电半径，排除电网节点自身，自动去重）
     * 核心等级不足的机器不计入（与调度逻辑一致）
     */
    private Set<BlockPos> collectPoweredMachines(List<BlockPos> nodes) {
        Set<BlockPos> machines = new LinkedHashSet<>();

        for (BlockPos node : nodes) {
            BlockEntity nodeBE = level.getBlockEntity(node);
            // 各节点供电半径不同（信号杆5×5×5，用电桩11×11×11）
            int radius = nodeBE instanceof PowerLinkable linkable ? linkable.getPowerRadius() : POWER_RADIUS;

            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos pos = node.offset(x, y, z);
                        if (machines.contains(pos)) {
                            continue;
                        }
                        BlockEntity be = level.getBlockEntity(pos);
                        if (be == null || be == nodeBE || be instanceof PowerLinkable) {
                            continue;
                        }
                        if (be instanceof PowerGridMachine machine && machine.getRequiredCoreLevel() > coreLevel) {
                            continue;
                        }
                        if (be.getCapability(ForgeCapabilities.ENERGY).isPresent()) {
                            machines.add(pos);
                        }
                    }
                }
            }
        }

        return machines;
    }

    /**
     * 获取电网状态文本（右键显示）
     */
    public Component getNetworkStats() {
        List<BlockPos> nodes = collectNetworkNodes();
        Set<BlockPos> machines = collectPoweredMachines(nodes);

        return Component.translatable("message.alone_journey.power_core.stats",
                String.format("%,d", energy.getEnergyStored()),
                String.format("%,d", energy.getMaxEnergyStored()),
                nodes.size() - 1, // 信号杆数（不含核心自身）
                machines.size());
    }

    /**
     * 当前核心等级（0~5）
     */
    public int getCoreLevel() {
        return coreLevel;
    }

    /**
     * 设置核心等级并同步调整网络储能上限（升级时保留已有电量）
     */
    public void setCoreLevel(int level) {
        this.coreLevel = Mth.clamp(level, 0, MAX_CORE_LEVEL);
        energy.setCapacity(NETWORK_CAPACITY * (this.coreLevel + 1));
        setChanged();
    }

    /**
     * 判断是否为当前等级可用的升级组件：
     * 等级 N（未满级）只接受 N+1 级组件
     */
    public boolean isUpgradeItem(ItemStack stack) {
        return coreLevel < MAX_CORE_LEVEL
                && stack.is(core_upgrade.forLevel(coreLevel + 1));
    }

    /**
     * 判断是否为专属核心升级插件
     * TODO 专属插件系统：各插件提供独立加成
     */
    public boolean isPluginItem(ItemStack stack) {
        return false;
    }

    public NetworkEnergyStorage getEnergyStorage() {
        return energy;
    }

    public ItemStackHandler getUpgradeInventory() {
        return upgradeInventory;
    }

    /**
     * 破坏方块时掉落内部物品（插件等常驻槽位物品）
     */
    public void dropInventory(Level level, BlockPos pos) {
        for (int i = 0; i < upgradeInventory.getSlots(); i++) {
            ItemStack stack = upgradeInventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
    }

    public ContainerData getMenuData() {
        return menuData;
    }

    // 能力暴露（护目镜 HUD / 外部只读查询电量）

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyLazy.cast();
        }
        return super.getCapability(cap, side);
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

    // MenuProvider 接口实现
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.alone_journey.power_core");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PowerCoreMenu(containerId, playerInventory, worldPosition);
    }

    // PowerLinkable 接口实现

    @Override
    public void addConnection(BlockPos pos) {
        if (!connections.contains(pos)) {
            connections.add(pos.immutable());
            setChanged();
            syncToClient();
        }
    }

    @Override
    public void removeConnection(BlockPos pos) {
        if (connections.remove(pos)) {
            setChanged();
            syncToClient();
        }
    }

    @Override
    public boolean isConnected(BlockPos pos) {
        return connections.contains(pos);
    }

    @Override
    public List<BlockPos> getConnections() {
        return connections;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        // 注意：EnergyStorage 序列化产物是 IntTag 而非 CompoundTag，
        // 必须用 putInt/getInt 存取，getCompound 读不回来
        tag.putInt(TAG_ENERGY, energy.getEnergyStored());
        tag.putInt(TAG_CORE_LEVEL, coreLevel);
        tag.put(TAG_UPGRADE_INVENTORY, upgradeInventory.serializeNBT());

        ListTag list = new ListTag();
        for (BlockPos pos : connections) {
            list.add(LongTag.valueOf(pos.asLong()));
        }
        tag.put(TAG_CONNECTIONS, list);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        // 注意顺序：先恢复等级（联动恢复容量），再恢复电量
        // 否则电量会被默认容量（1M）截断，高等级核心读档丢电
        setCoreLevel(tag.contains(TAG_CORE_LEVEL) ? tag.getInt(TAG_CORE_LEVEL) : 0);
        if (tag.contains(TAG_ENERGY)) {
            energy.setEnergy(tag.getInt(TAG_ENERGY));
        }
        if (tag.contains(TAG_UPGRADE_INVENTORY)) {
            upgradeInventory.deserializeNBT(tag.getCompound(TAG_UPGRADE_INVENTORY));
        }

        connections.clear();
        ListTag list = tag.getList(TAG_CONNECTIONS, Tag.TAG_LONG);
        for (Tag t : list) {
            if (t instanceof LongTag longTag) {
                connections.add(BlockPos.of(longTag.getAsLong()));
            }
        }
        syncWireRegistry();

        // 吞吐统计仅内存保留，读档后重新累积
        lastSyncedPowered = energy.getEnergyStored() > 0;
    }

    /**
     * 连接变化后广播方块更新，使客户端收到最新的连接数据（渲染电线）
     */
    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // 方块实体数据同步（客户端渲染电线依赖连接列表）

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setLevel(@NotNull Level level) {
        super.setLevel(level);
        syncWireRegistry();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        removeWireRegistry();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        syncWireRegistry();
    }

    /**
     * 电网内部储能：由核心统一调度输入输出
     */
    public static class NetworkEnergyStorage extends EnergyStorage {

        public NetworkEnergyStorage(int capacity) {
            super(capacity, 0, 0);
        }

        /**
         * 网络收入电力（从发电设备抽取）
         */
        public int produceEnergy(int amount) {
            int received = Math.min(amount, getMaxEnergyStored() - this.energy);
            if (received > 0) {
                this.energy += received;
            }
            return received;
        }

        /**
         * 网络支出电力（向用电设备输送）
         */
        public int drainEnergy(int amount) {
            int drained = Math.min(amount, this.energy);
            if (drained > 0) {
                this.energy -= drained;
            }
            return drained;
        }

        /**
         * 读档时直接设置当前电量
         */
        public void setEnergy(int value) {
            this.energy = Mth.clamp(value, 0, getMaxEnergyStored());
        }

        /**
         * 客户端同步用：直接设置当前电量，不按当前容量钲位
         * （容量数据可能晚于电量数据到达）
         */
        public void setEnergyClient(int value) {
            this.energy = Math.max(0, value);
        }

        /**
         * 调整网络储能上限（降级时裁剪超出电量）
         */
        public void setCapacity(int capacity) {
            this.capacity = Math.max(1, capacity);
            if (this.energy > this.capacity) {
                this.energy = this.capacity;
            }
        }
    }
}
