package Alone818.com.alone_journey.network;

import Alone818.com.alone_journey.Alone_journey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 模组网络通道
 */
public class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Alone_journey.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    public static void register() {
        int id = 0;

        // 客户端 -> 服务端：电网连接按键
        CHANNEL.messageBuilder(PowerLinkPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(PowerLinkPacket::decode)
                .encoder(PowerLinkPacket::encode)
                .consumerMainThread(PowerLinkPacket::handle)
                .add();

        // 服务端 -> 客户端：拉线起点高亮
        CHANNEL.messageBuilder(LinkHighlightPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(LinkHighlightPacket::decode)
                .encoder(LinkHighlightPacket::encode)
                .consumerMainThread(LinkHighlightPacket::handle)
                .add();
    }

    /**
     * 发送数据包到服务端
     */
    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    /**
     * 发送数据包到指定玩家客户端
     */
    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
