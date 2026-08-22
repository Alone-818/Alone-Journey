package Alone818.com.alone_journey.blockentities;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 电网节点电线渲染注册表（客户端持有）
 *
 * 记录每个电网节点（控制核心/信号杆/用电桩）的连接列表，供电线渲染器绘制黑弧线；
 * 仅包含纯数据，不含客户端专属类，服务端不会写入
 */
public class PylonWireRegistry {

    // key = 电网节点位置 asLong，value = 连接列表快照
    private static final Map<Long, List<BlockPos>> WIRES = new HashMap<>();

    public static void update(BlockPos pos, List<BlockPos> connections) {
        if (connections == null || connections.isEmpty()) {
            WIRES.remove(pos.asLong());
        } else {
            WIRES.put(pos.asLong(), new ArrayList<>(connections));
        }
    }

    public static void remove(BlockPos pos) {
        WIRES.remove(pos.asLong());
    }

    public static void clear() {
        WIRES.clear();
    }

    public static Map<Long, List<BlockPos>> view() {
        return Collections.unmodifiableMap(WIRES);
    }
}
