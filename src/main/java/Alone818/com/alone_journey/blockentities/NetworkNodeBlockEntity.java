package Alone818.com.alone_journey.blockentities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 电网被动节点基类（信号杆/用电桩共用）
 *
 * 功能特性：
 * - 与控制核心及其他节点通过电力连接组成电网
 * - 被动节点：不参与能量调度，由控制核心统一管理
 * - 服务端每秒沿连接BFS寻找控制核心，缓存电网总电量（供界面显示）
 */
public abstract class NetworkNodeBlockEntity extends BlockEntity implements MenuProvider, PowerLinkable {

    // 电网统计刷新周期（tick）与BFS搜索上限
    private static final int STATS_INTERVAL = 20;
    private static final int MAX_SEARCH_NODES = 64;

    // 电力连接的节点位置列表
    protected final List<BlockPos> connections = new ArrayList<>();

    // 电网统计缓存（服务端周期刷新）
    private int statsTimer = 0;
    private boolean hasNetwork = false;
    private int cachedNetworkEnergy = 0;
    private int cachedNetworkCapacity = 0;

    // 电网吞吐缓存（FE/s，索引 0=即时 1=5分钟 2=10分钟，由核心统计同步）
    private final int[] cachedRateIn = new int[3];
    private final int[] cachedRateOut = new int[3];

    // 界面同步数据
    // 索引：0=是否接入电网 1=电网总电量 2=电网储能上限
    //      3=即时输入 4=即时输出 5=5分钟输入 6=5分钟输出 7=10分钟输入 8=10分钟输出（FE/s）
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> hasNetwork ? 1 : 0;
                case 1 -> cachedNetworkEnergy;
                case 2 -> cachedNetworkCapacity;
                case 3, 5, 7 -> cachedRateIn[(index - 3) / 2];
                case 4, 6, 8 -> cachedRateOut[(index - 4) / 2];
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // 客户端接收服务端同步数据时写入（服务端不会调用此方法）
            switch (index) {
                case 0 -> hasNetwork = value > 0;
                case 1 -> cachedNetworkEnergy = value;
                case 2 -> cachedNetworkCapacity = value;
                case 3, 5, 7 -> cachedRateIn[(index - 3) / 2] = value;
                case 4, 6, 8 -> cachedRateOut[(index - 4) / 2] = value;
            }
        }

        @Override
        public int getCount() {
            return 9;
        }
    };

    protected NetworkNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void tick() {
        if (level == null || level.isClientSide()) {
            return;
        }

        // 每秒刷新一次电网缓存（供界面显示）
        if (statsTimer++ >= STATS_INTERVAL) {
            statsTimer = 0;
            refreshNetworkCache();
        }
    }

    /**
     * 沿电力连接BFS寻找电网中的控制核心，缓存其总电量与储能上限
     */
    private void refreshNetworkCache() {
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        queue.add(worldPosition);
        visited.add(worldPosition);

        while (!queue.isEmpty() && visited.size() <= MAX_SEARCH_NODES) {
            BlockPos pos = queue.poll();
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof PowerLinkable linkable)) {
                // 连接失效（方块被破坏/替换），跳过
                continue;
            }

            if (be instanceof PowerCoreBlockEntity core) {
                hasNetwork = true;
                cachedNetworkEnergy = core.getEnergyStorage().getEnergyStored();
                cachedNetworkCapacity = core.getEnergyStorage().getMaxEnergyStored();
                // 吞吐缓存随核心统计同步（即时/5分钟/10分钟）
                for (int mode = 0; mode < 3; mode++) {
                    cachedRateIn[mode] = core.getRateIn(mode);
                    cachedRateOut[mode] = core.getRateOut(mode);
                }
                return;
            }

            for (BlockPos conn : linkable.getConnections()) {
                if (visited.add(conn)) {
                    queue.add(conn);
                }
            }
        }

        // 未找到任何控制核心
        hasNetwork = false;
        cachedNetworkEnergy = 0;
        cachedNetworkCapacity = 0;
    }

    public ContainerData getMenuData() {
        return menuData;
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

        ListTag list = new ListTag();
        for (BlockPos pos : connections) {
            list.add(LongTag.valueOf(pos.asLong()));
        }
        tag.put("Connections", list);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);

        connections.clear();
        ListTag list = tag.getList("Connections", Tag.TAG_LONG);
        for (Tag t : list) {
            if (t instanceof LongTag longTag) {
                connections.add(BlockPos.of(longTag.getAsLong()));
            }
        }
        onConnectionsLoaded();
    }

    /**
     * 连接数据载入后的钩子（子类可同步客户端渲染状态）
     */
    protected void onConnectionsLoaded() {
        syncWireRegistry();
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

    // MenuProvider：由子类提供具体界面
    @Override
    public abstract Component getDisplayName();
}
