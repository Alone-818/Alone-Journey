package Alone818.com.alone_journey.client;

import Alone818.com.alone_journey.Alone_journey;
import Alone818.com.alone_journey.blockentities.PowerGridMachine;
import Alone818.com.alone_journey.blockentities.PowerLinkable;
import Alone818.com.alone_journey.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;

/**
 * 工程师护目镜 HUD
 *
 * 佩戴护目镜且准星指向机器（有能量缓存的方块实体）或电网节点时，
 * 在准星左侧（带黑色半透明背景）显示：
 * - 机器名（所在方块名）
 * - 电量（有能量缓存时）
 * - 所需核心等级（PowerGridMachine）
 * - 供电范围：指向节点时显示其自身供电区域；
 *   指向机器时显示覆盖它的供电节点与该节点的供电区域
 */
@Mod.EventBusSubscriber(modid = Alone_journey.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GogglesHudOverlay {

    // HUD 文字颜色
    private static final int COLOR_TITLE = 0xFFFFFF;   // 机器名：白色
    private static final int COLOR_ENERGY = 0x00FFFF;  // 电量：青色
    private static final int COLOR_LEVEL = 0xA0A0A0;   // 核心等级：灰色
    private static final int COLOR_RANGE = 0x55FF55;   // 供电范围：绿色
    private static final int COLOR_NONE = 0xFF5555;    // 无供电：红色

    // 供电节点查找范围：供电半径最大5（用电桩），在此范围内扫描覆盖节点
    private static final int MAX_NODE_RADIUS = 5;

    // 供电节点查找缓存（准星目标变化时才重新扫描）
    private static BlockPos cachedTargetPos = null;
    @Nullable
    private static BlockPos cachedNodePos = null;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.hitResult == null) {
            return;
        }

        // 佩戴检查（Curios 头部槽）
        if (CuriosApi.getCuriosHelper().findCurios(player, ModItems.ENGINEER_GOGGLES.get()).isEmpty()) {
            return;
        }

        // 准星指向方块
        if (!(mc.hitResult instanceof BlockHitResult hit)
                || mc.hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        BlockEntity be = mc.level.getBlockEntity(pos);
        if (be == null) {
            return;
        }

        // 机器 = 电网节点 或 有能量缓存的方块实体
        IEnergyStorage energy = be.getCapability(ForgeCapabilities.ENERGY).orElse(null);
        if (!(be instanceof PowerLinkable) && energy == null) {
            return;
        }

        List<Component> lines = buildInfoLines(player, pos, be, energy);
        if (lines.isEmpty()) {
            return;
        }

        // 准星左侧绘制（右对齐到准星），带黑色半透明背景
        GuiGraphics gui = event.getGuiGraphics();
        Font font = mc.font;
        int lineHeight = font.lineHeight + 1;

        int maxWidth = 0;
        for (Component line : lines) {
            maxWidth = Math.max(maxWidth, font.width(line));
        }

        int gap = 8;      // 文字与准星的间距
        int padding = 3;  // 背景内边距
        int bgLeft = gui.guiWidth() / 2 - gap - maxWidth - padding;
        int bgHeight = lines.size() * lineHeight - 1 + padding * 2;
        int bgTop = gui.guiHeight() / 2 - bgHeight / 2;

        // 黑色半透明背景
        gui.fill(bgLeft, bgTop, bgLeft + maxWidth + padding * 2, bgTop + bgHeight, 0xA0000000);

        int textY = bgTop + padding;
        for (Component line : lines) {
            gui.drawString(font, line, bgLeft + padding, textY, 0xFFFFFF);
            textY += lineHeight;
        }
    }

    /**
     * 组装显示内容：机器名 / 电量 / 核心等级 / 供电范围
     */
    private static List<Component> buildInfoLines(Player player, BlockPos pos, BlockEntity be,
                                                  @Nullable IEnergyStorage energy) {
        List<Component> lines = new ArrayList<>();

        // 机器名（方块名）
        lines.add(player.level().getBlockState(pos).getBlock().getName());

        // 电量
        if (energy != null) {
            lines.add(Component.translatable("hud.alone_journey.goggles.energy",
                    String.format("%,d", energy.getEnergyStored()),
                    String.format("%,d", energy.getMaxEnergyStored())));
        }

        // 所需核心等级
        if (be instanceof PowerGridMachine machine) {
            lines.add(Component.translatable("hud.alone_journey.goggles.core_level",
                    machine.getRequiredCoreLevel()));
        }

        // 供电范围
        if (be instanceof PowerLinkable node) {
            lines.add(powerRangeLine(node.getPowerRadius()));
        } else {
            BlockPos nodePos = findCoveringNode(player, pos);
            if (nodePos != null
                    && player.level().getBlockEntity(nodePos) instanceof PowerLinkable node) {
                lines.add(Component.translatable("hud.alone_journey.goggles.powered_by",
                        player.level().getBlockState(nodePos).getBlock().getName(),
                        node.getPowerRadius() * 2 + 1));
                lines.add(powerRangeLine(node.getPowerRadius()));
            } else {
                lines.add(Component.translatable("hud.alone_journey.goggles.not_powered")
                        .withStyle(ChatFormatting.RED));
            }
        }
        return lines;
    }

    /**
     * 供电范围文字：N×N×N 供电区域
     */
    private static Component powerRangeLine(int radius) {
        int size = radius * 2 + 1;
        return Component.translatable("hud.alone_journey.goggles.power_radius", size, size, size);
    }

    /**
     * 查找覆盖目标方块的供电节点（目标在节点供电半径内，取最近的一个）
     *
     * 结果按目标位置缓存，避免每帧全量扫描
     */
    @Nullable
    private static BlockPos findCoveringNode(Player player, BlockPos target) {
        if (target.equals(cachedTargetPos)) {
            return cachedNodePos;
        }
        cachedTargetPos = target.immutable();

        BlockPos best = null;
        int bestDist = Integer.MAX_VALUE;
        for (int x = -MAX_NODE_RADIUS; x <= MAX_NODE_RADIUS; x++) {
            for (int y = -MAX_NODE_RADIUS; y <= MAX_NODE_RADIUS; y++) {
                for (int z = -MAX_NODE_RADIUS; z <= MAX_NODE_RADIUS; z++) {
                    BlockPos nodePos = target.offset(x, y, z);
                    if (Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z))) >= bestDist) {
                        continue;
                    }
                    if (player.level().getBlockEntity(nodePos) instanceof PowerLinkable node
                            // 目标必须在节点的供电半径内
                            && chebyshev(target, nodePos) <= node.getPowerRadius()) {
                        best = nodePos;
                        bestDist = Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z)));
                    }
                }
            }
        }
        cachedNodePos = best;
        return best;
    }

    /**
     * 两点切比雪夫距离（立方体供电区域）
     */
    private static int chebyshev(BlockPos a, BlockPos b) {
        return Math.max(Math.abs(a.getX() - b.getX()),
                Math.max(Math.abs(a.getY() - b.getY()), Math.abs(a.getZ() - b.getZ())));
    }
}
