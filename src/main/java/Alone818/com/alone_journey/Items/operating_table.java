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
 * 手术台方块物品
 */
public class operating_table extends BlockItem {

    public operating_table() {
        super(ModBlocks.OPERATING_TABLE.get(), new Item.Properties());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("block.alone_journey.operating_table.tooltip.desc").withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("block.alone_journey.operating_table.tooltip.tab.armor").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("block.alone_journey.operating_table.tooltip.armor_desc").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("block.alone_journey.operating_table.tooltip.tab.damage").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("block.alone_journey.operating_table.tooltip.damage_desc").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("block.alone_journey.operating_table.tooltip.tab.speed").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("block.alone_journey.operating_table.tooltip.speed_desc").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("block.alone_journey.operating_table.tooltip.tab.health").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("block.alone_journey.operating_table.tooltip.health_desc").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("block.alone_journey.operating_table.tooltip.tab.luck").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("block.alone_journey.operating_table.tooltip.luck_desc").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable("block.alone_journey.operating_table.tooltip.desc").withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("block.alone_journey.operating_table.tooltip.usage").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.alone_journey.press_shift").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}