package Alone818.com.alone_journey.Itemcuiros;

import java.util.List;
import java.util.UUID;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class bleedingshield extends Item implements ICurioItem {

    // 配置常量
    private static final double ARMOR_REDUCTION = 0.8;          // 减少的护甲值（点）
    private static final double MAX_HEALTH_REDUCTION = 0.2;     // 减少的最大生命值（比例，20%）
    private static final double TOUGHNESS_PER_UNIT = 0.5;       // 每减少 1 点护甲 + 此值护甲韧性


    public bleedingshield() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();

        // 1. 减少护甲值（固定点数）
        modifiers.put(Attributes.ARMOR, new AttributeModifier(
                uuid,
                "Bleeding Shield Armor Reduction",
                -ARMOR_REDUCTION,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        ));

        // 2. 减少最大生命值（比例）
        modifiers.put(Attributes.MAX_HEALTH, new AttributeModifier(
                uuid,
                "Bleeding Shield Max Health Reduction",
                -MAX_HEALTH_REDUCTION,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        ));

        modifiers.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(
                uuid,
                "Bleeding Shield Toughness Bonus",
                TOUGHNESS_PER_UNIT,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        ));

        return modifiers;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {

            tooltip.add(Component.translatable("item.alone_journey.bleedingshield.tooltip.desc").withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("item.alone_journey.bleedingshield.tooltip.toughness_desc").withStyle(ChatFormatting.GOLD));

    }
}

