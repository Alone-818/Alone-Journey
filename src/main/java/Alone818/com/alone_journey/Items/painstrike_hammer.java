package Alone818.com.alone_journey.Items;

import Alone818.com.alone_journey.init.ModEffects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class painstrike_hammer extends Item {

    // 命中后施加的易伤时长（10 秒）
    public static final int VULNERABILITY_DURATION_TICKS = 200;
    // 目标已处于易伤时的延长时长（5 秒）
    public static final int VULNERABILITY_EXTEND_TICKS = 100;

    public painstrike_hammer() {
        super(new Properties().durability(850));
    }

    /**
     * 命中实体时结算：造成伤害即施加易伤。
     * 目标没有易伤时施加 10 秒（等级 I）；已有易伤时延长 5 秒且等级 +1。
     */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        MobEffectInstance existing = target.getEffect(ModEffects.VULNERABILITY.get());
        if (existing == null) {
            // 全新施加：10 秒易伤
            target.addEffect(new MobEffectInstance(
                    ModEffects.VULNERABILITY.get(),
                    VULNERABILITY_DURATION_TICKS,
                    0));
        } else {
            // 已有易伤：以“当前剩余时长 + 5 秒、等级 +1”重新施加（保留原有的可见性等参数）
            target.removeEffect(ModEffects.VULNERABILITY.get());
            target.addEffect(new MobEffectInstance(
                    ModEffects.VULNERABILITY.get(),
                    existing.getDuration() + VULNERABILITY_EXTEND_TICKS,
                    existing.getAmplifier() + 1,
                    existing.isAmbient(),
                    existing.isVisible(),
                    existing.showIcon()));
        }
        stack.hurtAndBreak(1, attacker, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }

    // 武器属性：高伤害、慢攻速的重锤手感（+5 攻击伤害，攻速 0.8 次/秒）
    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();
        if (slot == EquipmentSlot.MAINHAND) {
            modifiers.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                    BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 11.0D, AttributeModifier.Operation.ADDITION));
            modifiers.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                    BASE_ATTACK_SPEED_UUID, "Weapon modifier", -3.6D, AttributeModifier.Operation.ADDITION));
        }
        return modifiers;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.alone_journey.painstrike_hammer.tooltip.desc")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("item.alone_journey.painstrike_hammer.tooltip.charge_desc")
                .withStyle(ChatFormatting.GOLD));
    }
}
