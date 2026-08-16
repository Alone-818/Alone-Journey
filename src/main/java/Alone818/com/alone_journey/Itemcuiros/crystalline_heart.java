package Alone818.com.alone_journey.Itemcuiros;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
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
    private static final int PER_HEALTH_TO_SHIELD = 4;   // 每 4 点最大生命值可转化为 1 点护盾
    private static final double MAX_HEALTH_PENALTY = -0.8; // 减少 80% 最大生命值

    public static final UUID HEALTH_MODIFIER_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

    public crystalline_heart() {
        super(new Properties().stacksTo(1));
    }

    /**
     * 通过 Curios 标准路径应用 -80% 最大生命值修饰符（Curios 会在 GUI 中自动显示属性提示）。
     * 注意：这里使用固定的 HEALTH_MODIFIER_UUID 而非传入的槽位 UUID，
     * 使其与 applyMaxHealthPenalty 的手动兜底共享同一 ID——
     * 原版 AttributeInstance 会跳过重复 ID，两条路径不会叠加。
     */
    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();
        // 减少 80% 最大生命值
        modifiers.put(Attributes.MAX_HEALTH, new AttributeModifier(
                HEALTH_MODIFIER_UUID,
                "Crystalline Heart Max Health Reduction",
                MAX_HEALTH_PENALTY,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        ));
        return modifiers;
    }

    /**
     * 手动兜底应用同一个修饰符（幂等：已存在时直接跳过，包括 Curios 已应用的那份）。
     * 用于 Curios 应用时序不可控或重登录后瞬时修饰符丢失的情况。
     */
    public static void applyMaxHealthPenalty(Player player) {
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null || attr.getModifier(HEALTH_MODIFIER_UUID) != null) return;
        attr.addTransientModifier(new AttributeModifier(
                HEALTH_MODIFIER_UUID,
                "Crystalline Heart Max Health Reduction",
                MAX_HEALTH_PENALTY,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        ));
    }

    public static void removeMaxHealthPenalty(Player player) {
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) {
            attr.removeModifier(HEALTH_MODIFIER_UUID);
        }
    }

    /**
     * 计算减少后的最大生命值（原最大生命值的 20%）。
     * Curios 应用修饰符与 onEquip 的先后顺序不可控：
     * 修饰符已生效时 getMaxHealth() 即为减少后的值；
     * 未生效时当前值为原值，需自行乘以 20%。
     */
    public static double getReducedMaxHealth(Player player) {
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        boolean applied = attr != null && attr.getModifier(HEALTH_MODIFIER_UUID) != null;
        return applied ? player.getMaxHealth() : player.getMaxHealth() * 0.2;
    }

    /**
     * 佩戴后如果当前生命值高于减少后的最大生命值（原最大生命值的 20%），扣血到该值。
     */
    public static void clampHealthToMax(Player player) {
        double reducedMax = getReducedMaxHealth(player);
        if (player.getHealth() > reducedMax) {
            player.setHealth((float) reducedMax);
        }
    }

    /**
     * 动态计算基础最大护盾（不含护甲加成）：被减少的生命值 / PER_HEALTH_TO_SHIELD。
     * 随玩家当前最大生命值实时变化（血量涨护盾上限涨），替代首次佩戴时冻结在 NBT 中的 MaxShield。
     */
    public static double getBaseMaxShield(Player player) {
        double reducedMaxHealth = getReducedMaxHealth(player);
        // 被减少的生命值 = 原最大生命值 * 0.8 = 减少后最大生命值 * 4
        double healthReduction = reducedMaxHealth * 4.0;
        return healthReduction / PER_HEALTH_TO_SHIELD;
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player)) return;

        CompoundTag tag = stack.getOrCreateTag();

        // 首次穿上时初始化：记录最大护盾和基础生命值减少量
        if (!tag.contains(NB_TAG_MAX_SHIELD)) {
            // 统一换算出减少后的最大生命值，不依赖 Curios 修饰符的应用时机
            double reducedMaxHealth = getReducedMaxHealth(player);
            // 被减少的生命值 = 原最大生命值 * 0.8 = 减少后最大生命值 * 4
            double healthReduction = reducedMaxHealth * 4.0;
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

        // 佩戴瞬间血量高于新上限（原最大生命值的 20%）时，扣血到新上限
        clampHealthToMax(player);
    }

    /**
     * 脱下时不清除 NBT，防止数据丢失；也不会重置 MaxShield。
     * 但需要移除最大生命值减少修饰符，防止手动兜底的那份残留
     * （Curios 自己应用的那份会由 Curios 在脱下时移除，此处按同一 UUID 再移除一次是安全的空操作）。
     */
    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (slotContext.entity() instanceof Player player) {
            removeMaxHealthPenalty(player);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {

            tooltip.add(Component.translatable("item.alone_journey.crystalline_heart.tooltip.desc")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            tooltip.add(Component.translatable("item.alone_journey.crystalline_heart.tooltip.heal_desc")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.alone_journey.crystalline_heart.tooltip.armor_bonus")
                    .withStyle(ChatFormatting.GRAY));



            }}
