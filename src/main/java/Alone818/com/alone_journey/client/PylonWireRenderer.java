package Alone818.com.alone_journey.client;

import Alone818.com.alone_journey.Alone_journey;
import Alone818.com.alone_journey.blockentities.PowerCoreBlockEntity;
import Alone818.com.alone_journey.blockentities.PowerLinkable;
import Alone818.com.alone_journey.blockentities.PylonWireRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 电网节点电线渲染
 *
 * 电网节点（控制核心/信号杆/用电桩）之间的电力连接渲染为加粗弧线：
 * - 挂点为两端节点顶端中心（挂点高度由 PowerLinkable#getWireAnchorHeight 提供）
 * - 弧线用抛物线近似悬链线，中点按水平距离下垂（重力效果）
 * - 连接数据由各方块实体同步到 PylonWireRegistry
 * - 颜色：正常连接且有电（电网电量>0）为绿色，否则为红色
 *   （有电状态由核心在跨越0边界时同步，客户端沿连接BFS扩散）
 */
@Mod.EventBusSubscriber(modid = Alone_journey.MODID, value = Dist.CLIENT)
public class PylonWireRenderer {

    // 线段分段数（越大越平滑）
    private static final int SEGMENTS = 12;
    // 下垂比例（每格水平距离下垂量）与最大下垂高度（幅度已减半）
    private static final float SAG_RATIO = 0.05F;
    private static final float MAX_SAG = 1.25F;

    // 电线颜色：有电（绿）/ 无电（红）
    private static final float POWERED_R = 0.3F;
    private static final float POWERED_G = 0.9F;
    private static final float POWERED_B = 0.4F;
    private static final float UNPOWERED_R = 0.9F;
    private static final float UNPOWERED_G = 0.3F;
    private static final float UNPOWERED_B = 0.3F;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || PylonWireRegistry.view().isEmpty()) {
            return;
        }

        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        var bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        // 加粗电线（线宽4，与拉线引导线同渲染类型）
        VertexConsumer consumer = bufferSource.getBuffer(ModRenderTypes.THICK_LINES);

        // 有电节点集合：从电量>0的核心出发沿连接BFS扩散
        Set<Long> powered = collectPoweredNodes(level);

        for (Map.Entry<Long, List<BlockPos>> entry : PylonWireRegistry.view().entrySet()) {
            BlockPos from = BlockPos.of(entry.getKey());
            if (!(level.getBlockEntity(from) instanceof PowerLinkable fromNode)) {
                continue;
            }

            for (BlockPos to : entry.getValue()) {
                // 连接是双向记录，仅画一次；仅连接电网节点与电网节点
                if (from.asLong() >= to.asLong()) {
                    continue;
                }
                if (!(level.getBlockEntity(to) instanceof PowerLinkable toNode)) {
                    continue;
                }

                boolean isPowered = powered.contains(from.asLong()) && powered.contains(to.asLong());
                float r = isPowered ? POWERED_R : UNPOWERED_R;
                float g = isPowered ? POWERED_G : UNPOWERED_G;
                float b = isPowered ? POWERED_B : UNPOWERED_B;

                renderCatenary(pose, cam, consumer, from, to,
                        fromNode.getWireAnchorHeight(), toNode.getWireAnchorHeight(), r, g, b);
            }
        }

        // 主动刷新线条缓冲：华丽画质（Fabulous）管线下原版不会在此阶段后自动刷新
        bufferSource.endBatch(ModRenderTypes.THICK_LINES);
    }

    /**
     * 收集有电的电网节点：找到电量>0的控制核心后，沿电力连接BFS标记整个电网
     * （核心的电量随连接数据一起同步，跨越0边界时核心会主动广播更新）
     */
    private static Set<Long> collectPoweredNodes(ClientLevel level) {
        Set<Long> powered = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        for (Long key : PylonWireRegistry.view().keySet()) {
            BlockPos pos = BlockPos.of(key);
            if (level.getBlockEntity(pos) instanceof PowerCoreBlockEntity core
                    && core.getEnergyStorage().getEnergyStored() > 0) {
                powered.add(pos.asLong());
                queue.add(pos);
            }
        }

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            if (!(level.getBlockEntity(pos) instanceof PowerLinkable node)) {
                continue;
            }
            for (BlockPos conn : node.getConnections()) {
                if (powered.add(conn.asLong())) {
                    queue.add(conn);
                }
            }
        }
        return powered;
    }

    /**
     * 渲染一条带重力下垂的弧线（抛物线近似悬链线）：
     * P(t) = lerp(A, B, t) - (0, sag * 4t(1-t), 0)
     */
    private static void renderCatenary(PoseStack pose, Vec3 cam, VertexConsumer consumer,
                                       BlockPos fromPos, BlockPos toPos,
                                       double fromAnchor, double toAnchor,
                                       float red, float green, float blue) {
        // 挂点：节点顶端中心
        float ax = fromPos.getX() + 0.5F;
        float ay = (float) (fromPos.getY() + fromAnchor);
        float az = fromPos.getZ() + 0.5F;
        float bx = toPos.getX() + 0.5F;
        float by = (float) (toPos.getY() + toAnchor);
        float bz = toPos.getZ() + 0.5F;

        // 水平距离决定下垂量
        double horizontal = Math.sqrt((bx - ax) * (bx - ax) + (bz - az) * (bz - az));
        float sag = Math.min((float) (horizontal * SAG_RATIO), MAX_SAG);

        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();

        float prevX = 0, prevY = 0, prevZ = 0;
        for (int i = 0; i <= SEGMENTS; i++) {
            float t = (float) i / SEGMENTS;
            // 抛物线下垂：中点下垂最多，两端为0
            float dip = sag * 4.0F * t * (1 - t);
            float x = ax + (bx - ax) * t - (float) cam.x;
            float y = ay + (by - ay) * t - dip - (float) cam.y;
            float z = az + (bz - az) * t - (float) cam.z;

            if (i > 0) {
                drawSegment(consumer, matrix, normal, prevX, prevY, prevZ, x, y, z, red, green, blue);
            }
            prevX = x;
            prevY = y;
            prevZ = z;
        }
    }

    /**
     * 绘制一小段线（顶点法线 = 线段方向）
     */
    private static void drawSegment(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                                    float ax, float ay, float az, float bx, float by, float bz,
                                    float red, float green, float blue) {
        float dx = bx - ax;
        float dy = by - ay;
        float dz = bz - az;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-6) {
            return;
        }
        float nx = (float) (dx / length);
        float ny = (float) (dy / length);
        float nz = (float) (dz / length);

        consumer.vertex(matrix, ax, ay, az).color(red, green, blue, 1.0F).normal(normal, nx, ny, nz).endVertex();
        consumer.vertex(matrix, bx, by, bz).color(red, green, blue, 1.0F).normal(normal, nx, ny, nz).endVertex();
    }
}
