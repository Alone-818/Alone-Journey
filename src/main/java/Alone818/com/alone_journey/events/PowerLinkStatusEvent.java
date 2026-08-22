package Alone818.com.alone_journey.events;

import Alone818.com.alone_journey.blockentities.PowerLinkHelper;
import Alone818.com.alone_journey.blockentities.PowerLinkable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 电力连接状态提示事件
 *
 * 玩家处于拉线进行中时，在生命值上方（actionbar）实时显示
 * 与连接起点的距离（范围内绿色 / 超出红色）
 */
public class PowerLinkStatusEvent {

    // 提示刷新间隔（tick）
    private static final int REFRESH_INTERVAL = 5;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) {
            return;
        }

        Player player = event.player;
        BlockPos start = PowerLinkHelper.getPendingStart(player);
        if (start == null) {
            return;
        }

        if (player.tickCount % REFRESH_INTERVAL != 0) {
            return;
        }

        // 玩家当前位置与起点的距离（信号杆起点上限80格，其他40格）
        double distance = Math.sqrt(player.blockPosition().distSqr(start));
        int maxDistance = PowerLinkHelper.getMaxLinkDistance(player.level(), start);
        boolean inRange = distance <= maxDistance;

        player.displayClientMessage(Component.translatable(
                        "message.alone_journey.power_link.distance",
                        String.format("%.1f", distance),
                        maxDistance)
                        .withStyle(inRange ? ChatFormatting.GREEN : ChatFormatting.RED), true);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // 登出时清除进行中的拉线，避免残留过期起点
        PowerLinkHelper.clearPendingLink(event.getEntity());
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // 拉线进行中：潜行右键任意方块取消连接
        if (event.getLevel().isClientSide() || !event.getEntity().isShiftKeyDown()) {
            return;
        }
        if (!PowerLinkHelper.hasPendingLink(event.getEntity())) {
            return;
        }
        // 点击信号杆/控制核心时交给方块自身的取消逻辑（同时不打开界面）
        if (event.getLevel().getBlockEntity(event.getPos()) instanceof PowerLinkable) {
            return;
        }

        if (PowerLinkHelper.cancelPendingLink(event.getEntity())) {
            // 阻断本次方块交互（避免取消同时放置方块等）
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}
