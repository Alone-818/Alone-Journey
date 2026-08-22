package Alone818.com.alone_journey.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * 拉线起点高亮数据包（服务端 -> 客户端）
 *
 * active=true 时客户端高亮该起点方块，false 时取消高亮
 */
public class LinkHighlightPacket {

    private final boolean active;
    private final BlockPos pos;

    public LinkHighlightPacket(@Nullable BlockPos pos) {
        this.active = pos != null;
        this.pos = pos == null ? BlockPos.ZERO : pos.immutable();
    }

    public static void encode(LinkHighlightPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.active);
        if (packet.active) {
            buf.writeBlockPos(packet.pos);
        }
    }

    public static LinkHighlightPacket decode(FriendlyByteBuf buf) {
        return buf.readBoolean() ? new LinkHighlightPacket(buf.readBlockPos()) : new LinkHighlightPacket(null);
    }

    public static void handle(LinkHighlightPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (packet.active) {
                LinkHighlightState.setStart(packet.pos);
            } else {
                LinkHighlightState.clear();
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * 发送给指定玩家
     */
    public static void sendTo(ServerPlayer player, @Nullable BlockPos pos) {
        ModNetwork.sendToPlayer(player, new LinkHighlightPacket(pos));
    }
}
