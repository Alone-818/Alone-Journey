package Alone818.com.alone_journey.blockentities;

import Alone818.com.alone_journey.network.LinkHighlightPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 电力连接管理器
 *
 * 连接方式（通过各方块界面的连接按键触发，经 PowerLinkPacket 到达服务端）：
 * - 点击信号杆/控制核心界面的连接按键 -> 开始电力连接（拉线）
 * - 打开另一个信号杆/控制核心界面点击其连接按键 -> 完成连接（距离不超过40格）
 * - 再次点击起点界面的连接按键 -> 取消连接
 *
 * 连接为双向记录，破坏任意节点时由方块自动清理两侧连接
 */
public class PowerLinkHelper {

    /**
     * 连接最大距离（格）：起点为信号杆80格，其他节点（用电桩/控制核心）40格
     */
    public static final int MAX_LINK_DISTANCE = 40;
    public static final int SIGNAL_POLE_MAX_LINK_DISTANCE = 80;

    /**
     * 获取从指定起点开始拉线的最大允许距离
     * （信号杆起点80格，用电桩/控制核心起点40格）
     */
    public static int getMaxLinkDistance(Level level, BlockPos start) {
        return level.getBlockEntity(start) instanceof SignalPoleBlockEntity
                ? SIGNAL_POLE_MAX_LINK_DISTANCE : MAX_LINK_DISTANCE;
    }

    // 进行中的电力连接（玩家 -> 起点坐标）
    // 仅保存拉线中间状态，服务器重启失效无碍
    private static final Map<UUID, BlockPos> PENDING_LINKS = new HashMap<>();

    /**
     * 获取玩家进行中的电力连接起点（无则返回 null，供状态提示使用）
     */
    @Nullable
    public static BlockPos getPendingStart(Player player) {
        return PENDING_LINKS.get(player.getUUID());
    }

    /**
     * 判断玩家是否处于拉线进行中
     */
    public static boolean hasPendingLink(Player player) {
        return PENDING_LINKS.containsKey(player.getUUID());
    }

    /**
     * 取消玩家的进行中连接（潜行右键触发，返回是否实际取消）
     */
    public static boolean cancelPendingLink(Player player) {
        if (PENDING_LINKS.remove(player.getUUID()) != null) {
            sendHighlight(player, null);
            player.sendSystemMessage(Component.translatable("message.alone_journey.power_link.cancel"));
            return true;
        }
        return false;
    }

    /**
     * 清除玩家的进行中连接（登出时调用）
     */
    public static void clearPendingLink(Player player) {
        PENDING_LINKS.remove(player.getUUID());
    }

    /**
     * 处理潜行右键的电力连接点击
     */
    public static InteractionResult handleLinkClick(Player player, Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockPos start = PENDING_LINKS.get(player.getUUID());

        // 没有进行中的连接：以该方块为起点开始拉线
        if (start == null) {
            PENDING_LINKS.put(player.getUUID(), pos.immutable());
            sendHighlight(player, pos);
            player.sendSystemMessage(Component.translatable(
                    "message.alone_journey.power_link.start", getMaxLinkDistance(level, pos)));
            return InteractionResult.SUCCESS;
        }

        // 点击起点：取消连接
        if (start.equals(pos)) {
            PENDING_LINKS.remove(player.getUUID());
            sendHighlight(player, null);
            player.sendSystemMessage(Component.translatable("message.alone_journey.power_link.cancel"));
            return InteractionResult.SUCCESS;
        }

        // 完成连接：校验两端
        BlockEntity startBE = level.getBlockEntity(start);
        BlockEntity targetBE = level.getBlockEntity(pos);
        if (!(startBE instanceof PowerLinkable startNode) || !(targetBE instanceof PowerLinkable targetNode)) {
            PENDING_LINKS.remove(player.getUUID());
            sendHighlight(player, null);
            player.sendSystemMessage(Component.translatable("message.alone_journey.power_link.fail_invalid"));
            return InteractionResult.SUCCESS;
        }

        // 距离校验（信号杆起点80格，其他40格；失败保留起点，可继续选择其他目标）
        int maxDistance = getMaxLinkDistance(level, start);
        if (start.distSqr(pos) > (double) maxDistance * maxDistance) {
            player.sendSystemMessage(Component.translatable(
                    "message.alone_journey.power_link.fail_distance", maxDistance));
            return InteractionResult.SUCCESS;
        }

        // 重复连接校验
        if (startNode.isConnected(pos)) {
            player.sendSystemMessage(Component.translatable("message.alone_journey.power_link.fail_exists"));
            return InteractionResult.SUCCESS;
        }

        // 双向记录连接
        startNode.addConnection(pos.immutable());
        targetNode.addConnection(start.immutable());
        PENDING_LINKS.remove(player.getUUID());
        sendHighlight(player, null);
        player.sendSystemMessage(Component.translatable("message.alone_journey.power_link.success"));
        return InteractionResult.SUCCESS;
    }

    /**
     * 通知客户端高亮/取消高亮拉线起点
     */
    private static void sendHighlight(Player player, @Nullable BlockPos pos) {
        if (player instanceof ServerPlayer serverPlayer) {
            LinkHighlightPacket.sendTo(serverPlayer, pos);
        }
    }
}
