package Alone818.com.alone_journey.client;

import Alone818.com.alone_journey.Alone_journey;
import Alone818.com.alone_journey.Itemcuiros.crystalline_heart;
import Alone818.com.alone_journey.init.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Optional;

/**
 * 客户端 HUD：在原版生命值心形图标的左侧绘制剩余护盾。
 * 显示形式为单个盾形图标 + *数值（如 *12），护盾值很大时也不会遮挡其他 HUD 元素。
 * 支持受击闪白与护盾耗尽置灰提示。
 */
@Mod.EventBusSubscriber(modid = Alone_journey.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ShieldHudOverlay {

    // 图标精灵表：36x9，从左到右依次为 空容器(0) / 满盾(9) / 半盾(18) / 闪白(27)
    private static final ResourceLocation SHIELD_ICONS =
            new ResourceLocation(Alone_journey.MODID, "textures/gui/shield_icons.png");
    private static final int ICON_SIZE = 9;       // 与原版心形图标相同的 9x9 规格
    private static final int ARMOR_TO_SHIELD = 5; // 与 ShieldEvent 保持一致：每 5 点护甲 +1 点护盾上限

    // 各精灵在纹理中的 u 坐标
    private static final int U_EMPTY = 0;
    private static final int U_FULL = 9;
    private static final int U_FLASH = 27;
    private static final int TEXTURE_WIDTH = 36;
    private static final int TEXTURE_HEIGHT = 9;

    // 颜色
    private static final int COLOR_SHIELD = 0xFF3BD3EE; // 水晶青
    private static final int COLOR_EMPTY = 0xFF8A8A8A;  // 护盾耗尽的灰色

    // 受击闪白状态（类似原版受击时心形高亮）
    private static double lastShield = -1.0;
    private static long flashUntilTick = Long.MIN_VALUE;

    @SubscribeEvent
    public static void onRenderHealth(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay().id() != VanillaGuiOverlay.PLAYER_HEALTH.id()) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        // 仅在生存和冒险模式下显示护盾（创造/旁观不显示）
        if (mc.gameMode == null) return;
        GameType gameType = mc.gameMode.getPlayerMode();
        if (gameType != GameType.SURVIVAL && gameType != GameType.ADVENTURE) return;

        GuiGraphics gui = event.getGuiGraphics();
        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // ===== 显示护盾信息 =====
        // 从客户端同步的饰品 NBT 中读取护盾数据
        Optional<SlotResult> opt = CuriosApi.getCuriosHelper()
                .findFirstCurio(player, ModItems.CRYSTALLINE_HEART.get());
        if (opt.isEmpty()) return;

        ItemStack stack = opt.get().stack();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(crystalline_heart.NB_TAG_SHIELD)) return;

        // 上限计算与服务端 ShieldEvent 保持一致
        double maxShield = tag.getDouble(crystalline_heart.NB_TAG_MAX_SHIELD)
                + player.getArmorValue() / (double) ARMOR_TO_SHIELD;
        double shield = tag.getDouble(crystalline_heart.NB_TAG_SHIELD);
        if (maxShield <= 0) return;
        shield = Mth.clamp(shield, 0, maxShield);

        // 护盾减少时触发闪白
        long gameTime = mc.level.getGameTime();
        if (lastShield >= 0 && shield < lastShield) {
            flashUntilTick = gameTime + 10;
        }
        lastShield = shield;
        boolean flashing = gameTime < flashUntilTick;
        boolean empty = shield <= 0;

        // 显示数值：剩余护盾点数（向上取整，半点也显示为 1）
        int shieldPoints = (int) Math.ceil(shield);
        String text = "*" + shieldPoints;

        // 原版血条首行心形位于 height - 39，护盾与血条同行、整体靠右对齐到血条左端的左侧
        int y = screenHeight - 39;
        int textWidth = font.width(text);
        int xStart = screenWidth / 2 - 91;
        int iconX = xStart - textWidth - ICON_SIZE - 3;
        int textX = iconX + ICON_SIZE + 1;

        // 盾形图标：护盾耗尽置灰，受击时闪白，否则为满盾
        int iconU = empty ? U_EMPTY : (flashing ? U_FLASH : U_FULL);
        gui.blit(SHIELD_ICONS, iconX, y, iconU, 0, ICON_SIZE, ICON_SIZE, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        // 数值文本：耗尽时灰色，受击闪白时白色，平时水晶青
        int color = empty ? COLOR_EMPTY : (flashing ? 0xFFFFFFFF : COLOR_SHIELD);
        gui.drawString(font, text, textX, y + 1, color);
    }
}
