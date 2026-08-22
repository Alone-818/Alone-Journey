package Alone818.com.alone_journey.blockentities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/**
 * 电网节点接口：控制核心与信号杆/用电桩实现，用于电力连接管理
 */
public interface PowerLinkable {

    /**
     * 添加电力连接
     */
    void addConnection(BlockPos pos);

    /**
     * 移除电力连接
     */
    void removeConnection(BlockPos pos);

    /**
     * 是否已与目标连接
     */
    boolean isConnected(BlockPos pos);

    /**
     * 获取所有连接的节点位置
     */
    List<BlockPos> getConnections();

    /**
     * 该节点的供电区域半径（默认2 = 5×5×5，用电桩为5 = 11×11×11）
     */
    default int getPowerRadius() {
        return 2;
    }

    /**
     * 电线挂点高度（方块底面到挂点的格数，用于电线渲染）
     */
    default double getWireAnchorHeight() {
        return 1.0;
    }

    /**
     * 客户端：把连接列表登记到电线渲染注册表（连接双方都会登记，渲染器按坐标去重）
     */
    default void syncWireRegistry() {
        if (this instanceof BlockEntity be
                && be.getLevel() != null && be.getLevel().isClientSide()) {
            PylonWireRegistry.update(be.getBlockPos(), getConnections());
        }
    }

    /**
     * 客户端：从电线渲染注册表移除自己（方块被移除时）
     */
    default void removeWireRegistry() {
        if (this instanceof BlockEntity be
                && be.getLevel() != null && be.getLevel().isClientSide()) {
            PylonWireRegistry.remove(be.getBlockPos());
        }
    }
}
