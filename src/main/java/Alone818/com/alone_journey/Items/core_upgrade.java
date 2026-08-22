package Alone818.com.alone_journey.Items;

import Alone818.com.alone_journey.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 核心升级组件物品（按等级区分，共5种）
 *
 * 每次升级需要不同等级的组件：核心等级 N 时放入 N+1 级组件
 * 才会被升级槽接受，放入后立即消耗，核心等级 +1（最高 5 级）
 */
public class core_upgrade extends Item {

    // 目标等级（1~5）：核心当前等级为 目标等级-1 时可用
    private final int targetLevel;

    public core_upgrade(int targetLevel) {
        super(new Item.Properties());
        this.targetLevel = targetLevel;
    }

    /**
     * 该组件升级到的核心等级
     */
    public int getTargetLevel() {
        return this.targetLevel;
    }

    /**
     * 各目标等级（1~5）对应的升级组件物品
     */
    public static Item forLevel(int targetLevel) {
        return switch (targetLevel) {
            case 1 -> ModItems.CORE_UPGRADE_1.get();
            case 2 -> ModItems.CORE_UPGRADE_2.get();
            case 3 -> ModItems.CORE_UPGRADE_3.get();
            case 4 -> ModItems.CORE_UPGRADE_4.get();
            default -> ModItems.CORE_UPGRADE_5.get();
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_journey.core_upgrade.tooltip.desc",
                    this.targetLevel).withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("item.alone_journey.core_upgrade.tooltip.usage",
                    this.targetLevel - 1, this.targetLevel).withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable("item.alone_journey.core_upgrade.tooltip.desc",
                    this.targetLevel).withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("tooltip.alone_journey.press_shift").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
