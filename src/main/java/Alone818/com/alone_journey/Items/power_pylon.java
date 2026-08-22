package Alone818.com.alone_journey.Items;

import Alone818.com.alone_journey.init.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 用电桩方块物品
 */
public class power_pylon extends BlockItem {

    public power_pylon() {
        super(ModBlocks.POWER_PYLON.get(), new Item.Properties());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("block.alone_journey.power_pylon.tooltip.desc").withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("block.alone_journey.power_pylon.tooltip.area").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable("block.alone_journey.power_pylon.tooltip.desc").withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("tooltip.alone_journey.press_shift").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
