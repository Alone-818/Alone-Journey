package Alone818.com.alone_journey.Itemcuiros;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/**
 * 工程师护目镜物品（Curios 头部饰品）
 *
 * 佩戴时准星指向机器/电网节点，在准星左侧（带黑色半透明背景）显示
 * 机器信息（电量、所需核心等级）与供电范围（HUD 见 GogglesHudOverlay）
 */
public class engineer_goggles extends Item implements ICurioItem {

    public engineer_goggles() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_journey.engineer_goggles.tooltip.desc")
                    .withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("item.alone_journey.engineer_goggles.tooltip.usage")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable("item.alone_journey.engineer_goggles.tooltip.desc")
                    .withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("tooltip.alone_journey.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
