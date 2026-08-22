package Alone818.com.alone_journey.client;

import Alone818.com.alone_journey.Alone_journey;
import Alone818.com.alone_journey.menus.PowerCoreMenu;
import Alone818.com.alone_journey.blockentities.PowerCoreBlockEntity;
import Alone818.com.alone_journey.network.ModNetwork;
import Alone818.com.alone_journey.network.PowerLinkPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 控制核心界面（信标风格布局）
 *
 * 界面布局（230×219，贴图坐标从左上角起算，贴图画布 256×256）：
 * - 左侧：电网信息显示区域 (13,10)~(112,91)
 * - 右侧：电网连接按键 (195,108) 19×18
 *   按键图标（19×18）：未按下 (0,219)，按下 (0,238)
 *   点击后向服务端发送拉线请求（等效于潜行右键该核心）
 * - 下方：玩家背包 + 快捷栏
 *
 * 背景贴图：assets/alone_journey/textures/gui/power_core.png
 */
public class PowerCoreScreen extends AbstractContainerScreen<PowerCoreMenu> {

    // 控制核心背景贴图
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Alone_journey.MODID, "textures/gui/power_core.png");

    // 电网信息显示区域左上角
    private static final int INFO_X = 13;
    private static final int INFO_Y = 10;
    private static final int INFO_LINE_HEIGHT = 11; // 行距

    // 内容文字颜色（亮蓝色）
    private static final int INFO_COLOR = 0x00FFFF;

    // 吞吐颜色（深色底板）：输入>输出 绿色 / 输入<输出 红色 / 相等 黄色
    private static final int COLOR_SURPLUS = 0x55FF55;
    private static final int COLOR_DEFICIT = 0xFF5555;
    private static final int COLOR_BALANCED = 0xFFFF55;

    // 当前吞吐显示模式（纯客户端切换）：0=即时 1=5分钟 2=10分钟
    private int rateMode = 0;

    // 电网连接按键（判定范围向右扩大1像素）
    private static final int BUTTON_X = 195;
    private static final int BUTTON_Y = 108;
    private static final int BUTTON_WIDTH = 20;   // 判定宽度（图标19 + 右侧1像素）
    private static final int BUTTON_HEIGHT = 18;
    private static final int ICON_WIDTH = 19;     // 图标实际宽度
    private static final int BUTTON_SRC_U = 0;
    private static final int BUTTON_V_NORMAL = 219; // 未按下图标
    private static final int BUTTON_V_PRESSED = 238; // 按下图标

    private PowerLinkButton linkButton;

    public PowerCoreScreen(PowerCoreMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 230;
        this.imageHeight = 219;
        this.titleLabelX = Integer.MAX_VALUE; // 隐藏标题，仅显示内容文字
        this.inventoryLabelY = this.imageHeight - 82; // 背包标签位置下移到玩家槽位上方
    }

    @Override
    protected void init() {
        super.init();
        this.linkButton = this.addRenderableWidget(
                new PowerLinkButton(this.leftPos + BUTTON_X, this.topPos + BUTTON_Y));
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // 背景贴图（含信息区域底板与按键底座）
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.drawNetworkInfo(guiGraphics);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // 模式行悬浮提示（点击切换显示模式）
        int modeX = this.leftPos + INFO_X + 5;
        int modeY = this.topPos + INFO_Y + 6 + INFO_LINE_HEIGHT * 4;
        if (mouseX >= modeX && mouseX < modeX + 90
                && mouseY >= modeY && mouseY < modeY + 10) {
            guiGraphics.renderTooltip(this.font,
                    Component.translatable("gui.alone_journey.network.throughput_hint"), mouseX, mouseY);
        }

        // 连接按键悬浮提示
        if (this.linkButton != null && this.linkButton.isHovered()) {
            guiGraphics.renderTooltip(this.font, this.linkButton.getMessage(), mouseX, mouseY);
        }
    }

    /**
     * 电网信息显示（左侧区域）：电量、信号杆数、网络机器数、核心等级、吞吐量
     */
    private void drawNetworkInfo(GuiGraphics guiGraphics) {
        int x = this.leftPos + INFO_X + 5;
        int y = this.topPos + INFO_Y + 6;

        guiGraphics.drawString(this.font,
                Component.translatable("gui.alone_journey.power_core.energy",
                        format(this.menu.getEnergyStored()), format(this.menu.getCapacity())),
                x, y, INFO_COLOR, false);
        guiGraphics.drawString(this.font,
                Component.translatable("gui.alone_journey.power_core.poles", this.menu.getPoleCount()),
                x, y + INFO_LINE_HEIGHT, INFO_COLOR, false);
        guiGraphics.drawString(this.font,
                Component.translatable("gui.alone_journey.power_core.machines", this.menu.getMachineCount()),
                x, y + INFO_LINE_HEIGHT * 2, INFO_COLOR, false);
        guiGraphics.drawString(this.font,
                Component.translatable("gui.alone_journey.power_core.level",
                        this.menu.getCoreLevel(), PowerCoreBlockEntity.MAX_CORE_LEVEL),
                x, y + INFO_LINE_HEIGHT * 3, INFO_COLOR, false);

        // 吞吐量：模式行 + 输入/输出速率（FE/s），颜色按 输入>输出 绿 / 输入<输出 红 / 相等 黄
        guiGraphics.drawString(this.font,
                Component.translatable("gui.alone_journey.network.throughput", rateModeName(this.rateMode)),
                x, y + INFO_LINE_HEIGHT * 4, INFO_COLOR, false);
        int in = this.menu.getRateIn(this.rateMode);
        int out = this.menu.getRateOut(this.rateMode);
        int color = in > out ? COLOR_SURPLUS : in < out ? COLOR_DEFICIT : COLOR_BALANCED;
        guiGraphics.drawString(this.font,
                Component.translatable("gui.alone_journey.network.rate_in", format(in)),
                x, y + INFO_LINE_HEIGHT * 5, color, false);
        guiGraphics.drawString(this.font,
                Component.translatable("gui.alone_journey.network.rate_out", format(out)),
                x, y + INFO_LINE_HEIGHT * 6, color, false);
    }

    /**
     * 吞吐显示模式名称（0=即时 1=5分钟 2=10分钟）
     */
    private static Component rateModeName(int mode) {
        return switch (mode) {
            case 1 -> Component.translatable("gui.alone_journey.rate_mode.5min");
            case 2 -> Component.translatable("gui.alone_journey.rate_mode.10min");
            default -> Component.translatable("gui.alone_journey.rate_mode.instant");
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 点击模式行循环切换：即时 -> 5分钟 -> 10分钟
        int modeX = this.leftPos + INFO_X + 5;
        int modeY = this.topPos + INFO_Y + 6 + INFO_LINE_HEIGHT * 4;
        if (button == 0
                && mouseX >= modeX && mouseX < modeX + 90
                && mouseY >= modeY && mouseY < modeY + 10) {
            this.rateMode = (this.rateMode + 1) % 3;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * 数字千分位格式化（如 100,000）
     */
    private static String format(int value) {
        return String.format("%,d", value);
    }

    /**
     * 电网连接按键
     *
     * 图标由背景贴图提供，按下状态与客户端记录的拉线开关一致：
     * 点击一次开始拉线（按下图标），再次点击取消（恢复图标），
     * 服务端逻辑与潜行右键核心完全一致
     */
    private class PowerLinkButton extends Button {

        // 是否处于拉线进行中（决定按下/未按下图标）
        private boolean active = false;

        PowerLinkButton(int x, int y) {
            super(x, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                    Component.translatable("gui.alone_journey.power_core.link_button"), b -> {},
                    Button.DEFAULT_NARRATION);
        }

        @Override
        public void onPress() {
            this.active = !this.active;
            ModNetwork.sendToServer(new PowerLinkPacket(PowerCoreScreen.this.menu.getPos()));
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int v = this.active ? BUTTON_V_PRESSED : BUTTON_V_NORMAL;
            guiGraphics.blit(TEXTURE, this.getX(), this.getY(), BUTTON_SRC_U, v, ICON_WIDTH, BUTTON_HEIGHT);
        }
    }
}
