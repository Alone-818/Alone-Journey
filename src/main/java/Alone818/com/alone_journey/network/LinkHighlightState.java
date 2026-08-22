package Alone818.com.alone_journey.network;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * 拉线起点高亮状态（客户端持有，网络包写入）
 */
public class LinkHighlightState {

    @Nullable
    private static BlockPos start;

    public static void setStart(@Nullable BlockPos pos) {
        start = pos == null ? null : pos.immutable();
    }

    public static void clear() {
        start = null;
    }

    @Nullable
    public static BlockPos getStart() {
        return start;
    }
}
