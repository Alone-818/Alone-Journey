package Alone818.com.alone_journey.network;

import Alone818.com.alone_journey.blockentities.PowerLinkable;
import Alone818.com.alone_journey.blockentities.PowerLinkHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 电网连接按键数据包（客户端 -> 服务端）
 *
 * 玩家在控制核心界面点击"电网连接"按键时发送，
 * 服务端复用 PowerLinkHelper 的拉线逻辑（等效于潜行右键该核心）
 */
public class PowerLinkPacket {

    // 控制核心位置
    private final BlockPos pos;

    public PowerLinkPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(PowerLinkPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
    }

    public static PowerLinkPacket decode(FriendlyByteBuf buf) {
        return new PowerLinkPacket(buf.readBlockPos());
    }

    public static void handle(PowerLinkPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            // 距离校验（防止利用数据包远程操作电网）
            if (player.blockPosition().distSqr(packet.pos) > 64.0 * 64.0) {
                return;
            }
            // 确认方块实体仍然是可连接节点（控制核心/信号杆）
            if (!(player.level().getBlockEntity(packet.pos) instanceof PowerLinkable)) {
                return;
            }
            PowerLinkHelper.handleLinkClick(player, player.level(), packet.pos);
        });
        ctx.get().setPacketHandled(true);
    }
}
