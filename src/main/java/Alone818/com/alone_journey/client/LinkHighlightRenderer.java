package Alone818.com.alone_journey.client;

import Alone818.com.alone_journey.Alone_journey;
import Alone818.com.alone_journey.blockentities.PowerLinkHelper;
import Alone818.com.alone_journey.blockentities.PowerLinkable;
import Alone818.com.alone_journey.network.LinkHighlightState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 拉线起点方块高亮渲染
 *
 * 玩家处于拉线进行中时：
 * - 在起点方块外绘制绿色线框轮廓
 * - 从起点方块中心向鼠标指向的方块中心绘制引导线
 * （状态由服务端 LinkHighlightPacket 同步到 LinkHighlightState）
 */
@Mod.EventBusSubscriber(modid = Alone_journey.MODID, value = Dist.CLIENT)
public class LinkHighlightRenderer {

    // 线框颜色（绿色 = 可连接 / 红色 = 不可连接）
    private static final float GREEN_R = 0.3F;
    private static final float GREEN_G = 1.0F;
    private static final float GREEN_B = 0.4F;
    private static final float RED_R = 1.0F;
    private static final float RED_G = 0.3F;
    private static final float RED_B = 0.3F;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        BlockPos pos = LinkHighlightState.getStart();
        if (pos == null) {
            return;
        }

        // 起点方块已被破坏/替换：取消高亮
        if (!(event.getCamera().getEntity().level().getBlockEntity(pos) instanceof PowerLinkable)) {
            LinkHighlightState.clear();
            return;
        }

        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        VertexConsumer consumer = Minecraft.getInstance().renderBuffers().bufferSource()
                .getBuffer(ModRenderTypes.THICK_LINES);

        // 起点方块线框轮廓
        pose.pushPose();
        pose.translate(pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z);
        LevelRenderer.renderLineBox(pose, consumer,
                -0.002, -0.002, -0.002, 1.002, 1.002, 1.002,
                GREEN_R, GREEN_G, GREEN_B, 1.0F);
        pose.popPose();

        // 鼠标指向方块时：起点中心 -> 指向方块中心的引导线
        // 指向可连接方块为绿色，否则为红色
        BlockPos target = getMouseTargetBlock();
        if (target != null && !target.equals(pos)) {
            boolean linkable = isLinkableTarget(pos, target);
            renderLinkLine(pose, cam, consumer,
                    Vec3.atLowerCornerOf(pos).add(0.5, 0.5, 0.5),
                    Vec3.atLowerCornerOf(target).add(0.5, 0.5, 0.5),
                    linkable ? GREEN_R : RED_R,
                    linkable ? GREEN_G : RED_G,
                    linkable ? GREEN_B : RED_B);
        }
    }

    /**
     * 判断目标方块当前是否可连接：
     * 是信号杆/用电桩/控制核心、未与起点连接过、距离在起点允许范围内
     * （信号杆起点80格，其他40格）
     */
    private static boolean isLinkableTarget(BlockPos start, BlockPos target) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }
        if (!(level.getBlockEntity(target) instanceof PowerLinkable targetNode)) {
            return false;
        }
        if (!(level.getBlockEntity(start) instanceof PowerLinkable startNode)) {
            return false;
        }
        if (startNode.isConnected(target)) {
            return false;
        }
        int maxDistance = PowerLinkHelper.getMaxLinkDistance(level, start);
        return start.distSqr(target) <= (double) maxDistance * maxDistance;
    }

    /**
     * 获取客户端当前准星指向的方块（非方块命中/无目标返回 null）
     */
    private static BlockPos getMouseTargetBlock() {
        HitResult hit = Minecraft.getInstance().hitResult;
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            return blockHit.getBlockPos();
        }
        return null;
    }

    /**
     * 绘制两点之间的引导线（顶点法线 = 线段方向，与 renderLineBox 的边线写法一致）
     */
    private static void renderLinkLine(PoseStack pose, Vec3 cam, VertexConsumer consumer,
                                       Vec3 from, Vec3 to, float r, float g, float b) {
        float ax = (float) (from.x - cam.x);
        float ay = (float) (from.y - cam.y);
        float az = (float) (from.z - cam.z);
        float bx = (float) (to.x - cam.x);
        float by = (float) (to.y - cam.y);
        float bz = (float) (to.z - cam.z);

        float dx = bx - ax;
        float dy = by - ay;
        float dz = bz - az;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-4) {
            return;
        }
        float nx = (float) (dx / length);
        float ny = (float) (dy / length);
        float nz = (float) (dz / length);

        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();

        consumer.vertex(matrix, ax, ay, az).color(r, g, b, 1.0F).normal(normal, nx, ny, nz).endVertex();
        consumer.vertex(matrix, bx, by, bz).color(r, g, b, 1.0F).normal(normal, nx, ny, nz).endVertex();
    }

    @SubscribeEvent
    public static void onClientLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        // 离开世界时清除本地高亮状态，避免残留
        LinkHighlightState.clear();
    }
}
