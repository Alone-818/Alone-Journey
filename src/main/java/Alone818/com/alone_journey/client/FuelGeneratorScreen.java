package Alone818.com.alone_journey.client;

import Alone818.com.alone_journey.Alone_journey;
import Alone818.com.alone_journey.menus.FuelGeneratorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * 燃料发电机界面（第二版布局，槽位由贴图绘制）
 *
 * 界面布局（176×166，贴图坐标从左上角起算）：
 * - 左侧：燃料槽 (24,28) 18×18
 * - 右侧：电量槽 (144,12) 19×49，从下往上填充
 *   电量填充贴图（满电状态）位于 (176,0) 19×49
 * - 中间：发电效率 / 燃烧速率文字显示，中心 x=105, y=20
 * - 中间下方：4个升级槽 (46,56) 起，横向间隔18像素
 *
 * 背景贴图：assets/alone_journey/textures/gui/fuel_generator.png（含槽位底板与电量槽底）
 * 仅电量填充由代码动态绘制
 */
public class FuelGeneratorScreen extends AbstractContainerScreen<FuelGeneratorMenu> {

    // 发电机背景贴图
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Alone_journey.MODID, "textures/gui/fuel_generator.png");

    // 电量槽显示区域（GUI 坐标）：19×49，从下往上填充
    private static final int ENERGY_BAR_X = 144;
    private static final int ENERGY_BAR_Y = 12;
    private static final int ENERGY_BAR_WIDTH = 19;
    private static final int ENERGY_BAR_HEIGHT = 49;

    // 电量填充贴图坐标（满电状态 19×49）
    private static final int ENERGY_SRC_X = 176;
    private static final int ENERGY_SRC_Y = 0;

    // 中间信息显示区域左上角
    private static final int INFO_X = 50;
    private static final int INFO_Y = 20;

    public FuelGeneratorScreen(FuelGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // 背景贴图（含槽位底板与电量槽底）
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 电量填充：取满电贴图底部对应区域，从下往上填充
        int max = Math.max(1, this.menu.getMaxEnergyStored());
        int fillHeight = Mth.clamp(this.menu.getEnergyStored() * ENERGY_BAR_HEIGHT / max, 0, ENERGY_BAR_HEIGHT);
        if (fillHeight > 0) {
            int offset = ENERGY_BAR_HEIGHT - fillHeight;
            guiGraphics.blit(TEXTURE, x + ENERGY_BAR_X, y + ENERGY_BAR_Y + offset,
                    ENERGY_SRC_X, ENERGY_SRC_Y + offset, ENERGY_BAR_WIDTH, fillHeight);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.drawInfoText(guiGraphics);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // 电量槽悬浮提示
        int barX = this.leftPos + ENERGY_BAR_X;
        int barY = this.topPos + ENERGY_BAR_Y;
        if (mouseX >= barX && mouseX < barX + ENERGY_BAR_WIDTH
                && mouseY >= barY && mouseY < barY + ENERGY_BAR_HEIGHT) {
            guiGraphics.renderTooltip(this.font, Component.translatable(
                            "gui.alone_journey.fuel_generator.energy",
                            format(this.menu.getEnergyStored()), format(this.menu.getMaxEnergyStored())),
                    mouseX, mouseY);
        }
    }

    /**
     * 中间信息显示：发电效率（FE/t）与燃烧速率
     * 左对齐绘制于 (60,20)，纯黑色、无阴影（原版默认字号）
     */
    private void drawInfoText(GuiGraphics guiGraphics) {
        int generation = this.menu.isBurning() ? this.menu.getCurrentGeneration() : 0;
        int x = this.leftPos + INFO_X;
        int y = this.topPos + INFO_Y;

        guiGraphics.drawString(this.font,
                Component.translatable("gui.alone_journey.fuel_generator.efficiency", format(generation)),
                x, y, 0x000000, false);
        guiGraphics.drawString(this.font,
                Component.translatable("gui.alone_journey.fuel_generator.burn_rate", this.menu.getBurnRate()),
                x, y + 12, 0x000000, false);
    }

    /**
     * 数字千分位格式化（如 100,000）
     */
    private static String format(int value) {
        return String.format("%,d", value);
    }
}
