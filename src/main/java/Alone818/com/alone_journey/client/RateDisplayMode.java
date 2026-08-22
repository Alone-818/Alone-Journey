package Alone818.com.alone_journey.client;

/**
 * 吞吐显示模式（客户端共享状态）
 *
 * 0=即时 1=5分钟 2=10分钟；控制核心与信号杆/用电桩界面共用，
 * 关闭界面后保留当前模式，下次打开继续使用
 */
public final class RateDisplayMode {

    // 显示模式总数
    public static final int MODES = 3;

    private static int mode = 0;

    private RateDisplayMode() {
    }

    /**
     * 当前显示模式（0=即时 1=5分钟 2=10分钟）
     */
    public static int get() {
        return mode;
    }

    /**
     * 切换到下一个显示模式（即时 -> 5分钟 -> 10分钟 -> 即时）
     */
    public static void next() {
        mode = (mode + 1) % MODES;
    }
}
