package Alone818.com.alone_journey.Itemcuiros;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.UUID;

public class crystalline_heart extends Item implements ICurioItem {

    // NBT 键
    public static final String NB_TAG_MAX_SHIELD = "MaxShield";      // 最大护盾值（首次穿上时记录，防止无限刷盾）
    public static final String NB_TAG_SHIELD = "ShieldReduction";     // 当前护盾值
    public static final String NB_TAG_LAST_HEAL = "LastHealTime";     // 上次回复护盾时的世界时间（tick）
    public static final String NB_TAG_HEALTH_REDUCTION = "HealthReduction"; // 已记录的生命值减少量

    // 配置常量
    private static final int HEAL_INTERVAL_TICKS = 200; // 每 10 秒（200 tick）回复 1 点护盾
    private static final int PER_HEALTH_TO_SHIELD = 4;   // 每 4 点最大生命值可转化为 1 点护盾

    private static final UUID HEALTH_MODIFIER_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

    public crystalline_heart() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();
        // 减少 80% 最大生命值
        modifiers.put(Attributes.MAX_HEALTH, new AttributeModifier(
                uuid,
                "Crystalline Heart Max Health Reduction",
                -0.8,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        ));
        return modifiers;
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player)) return;

        CompoundTag tag = stack.getOrCreateTag();

        // 首次穿上时初始化：记录最大护盾和基础生命值减少量
        if (!tag.contains(NB_TAG_MAX_SHIELD)) {
            double currentMaxHealth = player.getMaxHealth();
            double playerArmor =player.getArmorValue();
            // 被减少的生命值 = currentMaxHealth / 0.2 * 0.8 = currentMaxHealth * 4
            double healthReduction = currentMaxHealth * 4.0;
            tag.putDouble(NB_TAG_HEALTH_REDUCTION, healthReduction);
            // 最大护盾 = 被减少的生命值 / 4
            double maxShield = healthReduction / PER_HEALTH_TO_SHIELD;
            tag.putDouble(NB_TAG_MAX_SHIELD, maxShield);
            // 初始护盾 = 最大护盾
            tag.putDouble(NB_TAG_SHIELD, maxShield);
            // 首次穿上时不设置 LastHealTime，等第一次受伤后开始计时
            tag.remove(NB_TAG_LAST_HEAL);
        }
        // 重新穿上（之前已穿过的物品）：不重置 MaxShield，防止刷盾
    }

    /**
     * 脱下时不清除 NBT，防止数据丢失；也不会重置 MaxShield。
     */
    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {}

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {

            tooltip.add(Component.translatable("item.alone_journey.crystalline_heart.tooltip.desc")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            tooltip.add(Component.translatable("item.alone_journey.crystalline_heart.tooltip.heal_desc")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.alone_journey.crystalline_heart.tooltip.armor_bonus")
                    .withStyle(ChatFormatting.GRAY));



    }}
