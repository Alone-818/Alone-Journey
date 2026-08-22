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
 * 燃料发电机方块物品
 */
public class fuel_generator extends BlockItem {

    public fuel_generator() {
        super(ModBlocks.FUEL_GENERATOR.get(), new Item.Properties());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("block.alone_journey.fuel_generator.tooltip.desc").withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("block.alone_journey.fuel_generator.tooltip.generation").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("block.alone_journey.fuel_generator.tooltip.capacity").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("block.alone_journey.fuel_generator.tooltip.upgrade").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable("block.alone_journey.fuel_generator.tooltip.desc").withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("tooltip.alone_journey.press_shift").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
