package Alone818.com.alone_journey.menus;

import net.minecraft.core.BlockPos;

/**
 * 电网节点菜单公共接口（信号杆/用电桩界面共用同一屏幕）
 */
public interface NetworkStatusMenu {

    /**
     * 是否接入电网（找到控制核心）
     */
    boolean hasNetwork();

    /**
     * 电网总电量
     */
    int getNetworkEnergy();

    /**
     * 电网储能上限
     */
    int getNetworkCapacity();

    /**
     * 方块位置（用于连接按键数据包）
     */
    BlockPos getPos();

    /**
     * 电网输入吞吐速率（FE/s）
     *
     * @param mode 0=即时 1=5分钟 2=10分钟
     */
    int getRateIn(int mode);

    /**
     * 电网输出吞吐速率（FE/s）
     *
     * @param mode 0=即时 1=5分钟 2=10分钟
     */
    int getRateOut(int mode);
}
