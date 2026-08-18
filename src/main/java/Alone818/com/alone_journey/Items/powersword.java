package Alone818.com.alone_journey.Items;

import Alone818.com.alone_journey.Config;
import Alone818.com.alone_journey.init.ModEnchantments;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class powersword extends SwordItem {

    // 高护甲判定阈值：目标护甲值超过该值才开始结算穿甲加伤
    public static final int HIGH_ARMOR_THRESHOLD = 8;
    // 阈值之上每 1 点护甲附加的穿甲伤害
    public static final float PIERCE_DAMAGE_PER_ARMOR = 0.3F;

    // 高韧性判定阈值：目标护甲韧性超过该值的部分也参与穿甲加伤
    public static final float HIGH_ARMOR_TOUGHNESS_THRESHOLD = 6;
    // 韧性阈值之上每 1 点韧性附加的穿甲伤害
    public static final float PIERCE_DAMAGE_PER_ARMOR_TOUGHNESS = 0.5F;

    // 穿甲伤害上限
    public static final float PIERCE_DAMAGE_MAX = 15.0F;

    // 超频攻击伤害加成（ADDITION：+11 -> +17）
    public static final double OVERCLOCK_DAMAGE_BONUS = 6.0D;
    // 超频期间命中额外造成的火焰伤害
    public static final float OVERCLOCK_FIRE_DAMAGE = 3.0F;
    // 超频期间命中点燃目标的时长（秒）
    public static final int OVERCLOCK_IGNITE_SECONDS = 4;
    // 激活超频时的一次性耐久损耗
    public static final int OVERCLOCK_ACTIVATION_COST = 5;
    // 激活超频时获得的抗性等级（1 = 抗性 II）与持续时长（5 秒 = 100 tick）
    public static final int OVERCLOCK_RESISTANCE_AMPLIFIER = 1;
    public static final int OVERCLOCK_RESISTANCE_TICKS = 100;
    // 超频期间吸血比例：造成伤害的 80% 治疗持有者
    public static final float LIFESTEAL_RATIO = 0.8F;
    // 超频攻速/伤害修饰符的固定 UUID，用于施加/移除
    public static final UUID OVERCLOCK_DAMAGE_UUID =
            UUID.fromString("8d5f9b1a-3c2e-4f7a-9b6d-1e0c2a4b6d8f");
    // 超频状态标记：剩余 tick，> 0 表示处于超频（服务端权威，用于吸血/火焰判定）
    public static final String OVERCLOCK_TAG = "alone_journey_powersword_overclock";


    public powersword() {
        // 下界合金材质，伤害修饰 7：总攻击伤害 +11（下界合金剑为 +8）；攻速 -2.8（更慢更沉重）
        super(Tiers.NETHERITE, 7, -2.6F, new Properties().durability(2031).fireResistant());
    }

    /**
     * 单次右键激活超频：立即进入超频状态（持续时长由配置决定），
     * 施加攻击伤害加成，损耗少量耐久，并立刻进入技能冷却。
     * 剩余时间由 PowerSwordEvent 每 tick 递减，到期自动移除。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            player.getPersistentData().putInt(OVERCLOCK_TAG, Config.powerswordOverclockDuration);
            applyOverclockDamage(player);
            // 激活时获得抗性 II，持续 5 秒
            player.addEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_RESISTANCE, OVERCLOCK_RESISTANCE_TICKS,
                    OVERCLOCK_RESISTANCE_AMPLIFIER, false, true, true));
            stack.hurtAndBreak(OVERCLOCK_ACTIVATION_COST, player,
                    e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
            // 精工制造附魔：每级降低 10% 技能冷却
            int smithingLevel = stack.getEnchantmentLevel(ModEnchantments.METICULOUS_CRAFT.get());
            int cooldown = Config.powerswordOverclockCooldown;
            if (smithingLevel > 0) {
                cooldown = (int) (cooldown * (1.0 - smithingLevel * 0.10));
            }
            player.getCooldowns().addCooldown(this, cooldown);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * 施加超频攻击伤害修饰符（幂等：已存在时跳过；transient：不落盘）
     */
    public static void applyOverclockDamage(LivingEntity entity) {
        AttributeInstance attr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr == null || attr.getModifier(OVERCLOCK_DAMAGE_UUID) != null) {
            return;
        }
        attr.addTransientModifier(new AttributeModifier(
                OVERCLOCK_DAMAGE_UUID, "Power sword overclock",
                OVERCLOCK_DAMAGE_BONUS, AttributeModifier.Operation.ADDITION));
    }

    /**
     * 移除超频状态：清除标记并移除攻击伤害修饰符
     */
    public static void removeOverclock(LivingEntity entity) {
        entity.getPersistentData().remove(OVERCLOCK_TAG);
        AttributeInstance attr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr != null) {
            attr.removeModifier(OVERCLOCK_DAMAGE_UUID);
        }
    }

    /**
     * 是否处于超频模式（以服务端标记为准）
     */
    public static boolean hasOverclock(LivingEntity entity) {
        return entity.getPersistentData().getInt(OVERCLOCK_TAG) > 0;
    }

    /**
     * 命中结算：
     * 1. 穿甲：目标护甲/韧性超过阈值时附加无视护甲与韧性的魔法伤害。
     * 2. 超频期间：命中额外造成火焰伤害并点燃目标。
     * 3. 精工制造：每级额外增加 1.5 点伤害。
     */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide()) {
            // 计算精工制造附魔带来的额外伤害（每级 1.5 点）
            float meticulousBonus = stack.getEnchantmentLevel(ModEnchantments.METICULOUS_CRAFT.get()) * 1.5F;

            float armor = target.getArmorValue();
            float toughness = (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
            float pierce = Math.max(0F, armor - HIGH_ARMOR_THRESHOLD) * PIERCE_DAMAGE_PER_ARMOR
                    + Math.max(0F, toughness - HIGH_ARMOR_TOUGHNESS_THRESHOLD) * PIERCE_DAMAGE_PER_ARMOR_TOUGHNESS;
            if (pierce > 0F) {
                pierce = Math.min(PIERCE_DAMAGE_MAX, pierce);
                // 重置无敌帧，确保穿甲伤害不被主伤害吞掉
                target.invulnerableTime = 0;
                target.hurt(attacker.damageSources().indirectMagic(attacker, attacker), pierce);
            }

            if (hasOverclock(attacker)) {
                // 点燃 + 额外火焰伤害（重置无敌帧，确保不被吞掉）
                target.setSecondsOnFire(OVERCLOCK_IGNITE_SECONDS);
                target.invulnerableTime = 0;
                target.hurt(target.damageSources().onFire(), OVERCLOCK_FIRE_DAMAGE);
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        // 计算METICULOUS_CRAFT附魔的伤害加成
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_journey.powersword.tooltip.desc")
                    .withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.translatable("item.alone_journey.powersword.tooltip.pierce_desc")
                    .withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("item.alone_journey.powersword.tooltip.overclock_desc")
                    .withStyle(ChatFormatting.GOLD));

        } else {
            tooltip.add(Component.translatable("item.alone_journey.powersword.tooltip.desc")
                    .withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.translatable("tooltip.alone_journey.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
