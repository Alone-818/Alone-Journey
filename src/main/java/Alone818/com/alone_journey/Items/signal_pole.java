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
 * 信号杆方块物品
 */
public class signal_pole extends BlockItem {

    public signal_pole() {
        super(ModBlocks.SIGNAL_POLE.get(), new Item.Properties());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("block.alone_journey.signal_pole.tooltip.desc").withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("block.alone_journey.signal_pole.tooltip.area").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("block.alone_journey.signal_pole.tooltip.link").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable("block.alone_journey.signal_pole.tooltip.desc").withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("tooltip.alone_journey.press_shift").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
