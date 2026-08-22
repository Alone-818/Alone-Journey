package Alone818.com.alone_journey.client;

import Alone818.com.alone_journey.Alone_journey;
import Alone818.com.alone_journey.menus.NetworkStatusMenu;
import Alone818.com.alone_journey.network.ModNetwork;
import Alone818.com.alone_journey.network.PowerLinkPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * 电网节点通用界面（信号杆/用电桩共用，与发电机界面同风格）
 *
 * 界面布局（176×166，贴图坐标从左上角起算，贴图画布 256×256）：
 * - 左侧：电网总电量槽 (24,28)~(41,45)（贴图绘制）
 * - 右侧：电网总电量条 (144,12) 19×49，从下往上填充
 *   电量填充贴图（满电状态）位于 (176,0) 19×49
 * - 右侧：连接电线按键 (144,61) 19×18（判定范围向右扩大1像素）
 *   按键图标（19×18）：未按下 (176,51)，按下 (176,70)
 *
 * 背景贴图：assets/alone_journey/textures/gui/signal_pole.png
 */
public class NetworkNodeScreen<T extends AbstractContainerMenu & NetworkStatusMenu>
        extends AbstractContainerScreen<T> {

    // 节点背景贴图（信号杆/用电桩共用）
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Alone_journey.MODID, "textures/gui/signal_pole.png");

    // 电网总电量条显示区域（GUI 坐标）：19×49，从下往上填充（与发电机位置一致）
    private static final int ENERGY_BAR_X = 144;
    private static final int ENERGY_BAR_Y = 12;
    private static final int ENERGY_BAR_WIDTH = 19;
    private static final int ENERGY_BAR_HEIGHT = 49;

    // 电量填充贴图坐标（满电状态 19×49）
    private static final int ENERGY_SRC_X = 176;
    private static final int ENERGY_SRC_Y = 0;

    // 连接电线按键（判定范围向右扩大1像素，位置上移5像素）
    private static final int BUTTON_X = 144;
    private static final int BUTTON_Y = 61;
    private static final int BUTTON_WIDTH = 20;   // 判定宽度（图标19 + 右侧1像素）
    private static final int BUTTON_HEIGHT = 18;
    private static final int ICON_WIDTH = 19;     // 图标实际宽度
    private static final int BUTTON_SRC_U = 176;
    private static final int BUTTON_V_NORMAL = 51; // 未按下图标
    private static final int BUTTON_V_PRESSED = 70; // 按下图标

    private LinkButton linkButton;

    // 吞吐显示文字左上角（与发电机信息文字同位置），行距12像素
    private static final int RATE_X = 50;
    private static final int RATE_Y = 20;

    // 吞吐颜色：输入>输出 绿色 / 输入<输出 红色 / 相等 黄色
    private static final int COLOR_SURPLUS = 0x00AA00;
    private static final int COLOR_DEFICIT = 0xCC0000;
    private static final int COLOR_BALANCED = 0xAAAA00;

    // 当前吞吐显示模式（纯客户端切换）：0=即时 1=5分钟 2=10分钟
    private int rateMode = 0;

    public NetworkNodeScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.linkButton = this.addRenderableWidget(
                new LinkButton(this.leftPos + BUTTON_X, this.topPos + BUTTON_Y));
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // 背景贴图（含电量槽底与按键底座）
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 电网总电量填充：取满电贴图底部对应区域，从下往上填充
        if (this.menu.hasNetwork()) {
            int max = Math.max(1, this.menu.getNetworkCapacity());
            int fillHeight = Mth.clamp(this.menu.getNetworkEnergy() * ENERGY_BAR_HEIGHT / max, 0, ENERGY_BAR_HEIGHT);
            if (fillHeight > 0) {
                int offset = ENERGY_BAR_HEIGHT - fillHeight;
                guiGraphics.blit(TEXTURE, x + ENERGY_BAR_X, y + ENERGY_BAR_Y + offset,
                        ENERGY_SRC_X, ENERGY_SRC_Y + offset, ENERGY_BAR_WIDTH, fillHeight);
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.drawRateText(guiGraphics);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // 模式行悬浮提示（点击切换显示模式）
        if (mouseX >= this.leftPos + RATE_X && mouseX < this.leftPos + RATE_X + 80
                && mouseY >= this.topPos + RATE_Y && mouseY < this.topPos + RATE_Y + 10) {
            guiGraphics.renderTooltip(this.font,
                    Component.translatable("gui.alone_journey.network.throughput_hint"), mouseX, mouseY);
        }

        // 连接电线按键悬浮提示
        if (this.linkButton != null && this.linkButton.isHovered()) {
            guiGraphics.renderTooltip(this.font, this.linkButton.getMessage(), mouseX, mouseY);
        }

        // 电量条悬浮提示
        int barX = this.leftPos + ENERGY_BAR_X;
        int barY = this.topPos + ENERGY_BAR_Y;
        if (mouseX >= barX && mouseX < barX + ENERGY_BAR_WIDTH
                && mouseY >= barY && mouseY < barY + ENERGY_BAR_HEIGHT) {
            Component tooltip = this.menu.hasNetwork()
                    ? Component.translatable("gui.alone_journey.fuel_generator.energy",
                            format(this.menu.getNetworkEnergy()), format(this.menu.getNetworkCapacity()))
                    : Component.translatable("gui.alone_journey.signal_pole.no_network");
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    /**
     * 数字千分位格式化（如 100,000）
     */
    private static String format(int value) {
        return String.format("%,d", value);
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

    /**
     * 吞吐量文字显示（位置参考发电机信息文字）：
     * 未接入电网时提示；接入后显示 模式行 + 输入/输出速率（FE/s），
     * 颜色按 输入>输出 绿 / 输入<输出 红 / 相等 黄
     */
    private void drawRateText(GuiGraphics guiGraphics) {
        int x = this.leftPos + RATE_X;
        int y = this.topPos + RATE_Y;

        if (!this.menu.hasNetwork()) {
            guiGraphics.drawString(this.font,
                    Component.translatable("gui.alone_journey.signal_pole.no_network"),
                    x, y, 0x000000, false);
            return;
        }

        guiGraphics.drawString(this.font,
                Component.translatable("gui.alone_journey.network.throughput", rateModeName(this.rateMode)),
                x, y, 0x000000, false);

        int in = this.menu.getRateIn(this.rateMode);
        int out = this.menu.getRateOut(this.rateMode);
        int color = in > out ? COLOR_SURPLUS : in < out ? COLOR_DEFICIT : COLOR_BALANCED;
        guiGraphics.drawString(this.font,
                Component.translatable("gui.alone_journey.network.rate_in", format(in)),
                x, y + 12, color, false);
        guiGraphics.drawString(this.font,
                Component.translatable("gui.alone_journey.network.rate_out", format(out)),
                x, y + 24, color, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 点击模式行循环切换：即时 -> 5分钟 -> 10分钟
        if (button == 0
                && mouseX >= this.leftPos + RATE_X && mouseX < this.leftPos + RATE_X + 80
                && mouseY >= this.topPos + RATE_Y && mouseY < this.topPos + RATE_Y + 10) {
            this.rateMode = (this.rateMode + 1) % 3;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * 连接电线按键
     *
     * 图标由背景贴图提供，按下状态与客户端记录的拉线开关一致：
     * 点击一次开始拉线（按下图标），再次点击取消（恢复图标），
     * 服务端逻辑与在各节点界面点击连接按键完全一致
     */
    private class LinkButton extends Button {

        // 是否处于拉线进行中（决定按下/未按下图标）
        private boolean active = false;

        LinkButton(int x, int y) {
            super(x, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                    Component.translatable("gui.alone_journey.signal_pole.link_button"), b -> {},
                    Button.DEFAULT_NARRATION);
        }

        @Override
        public void onPress() {
            this.active = !this.active;
            ModNetwork.sendToServer(new PowerLinkPacket(NetworkNodeScreen.this.menu.getPos()));
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int v = this.active ? BUTTON_V_PRESSED : BUTTON_V_NORMAL;
            guiGraphics.blit(TEXTURE, this.getX(), this.getY(), BUTTON_SRC_U, v, ICON_WIDTH, BUTTON_HEIGHT);
        }
    }
}
