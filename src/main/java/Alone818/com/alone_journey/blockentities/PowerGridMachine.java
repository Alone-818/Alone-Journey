package Alone818.com.alone_journey.blockentities;

/**
 * 电网机器等级需求接口
 *
 * 接入电网的机器实现此接口，声明工作所需的最低核心等级：
 * - 核心等级不足时，机器不参与电网能量调度（不抽电也不送电）
 * - 等级需求 0 表示任何等级的核心都可用（如燃料发电机 0~5 级通用）
 */
public interface PowerGridMachine {

    /**
     * 该机器正常工作所需的最低核心等级（0~5）
     */
    int getRequiredCoreLevel();
}
