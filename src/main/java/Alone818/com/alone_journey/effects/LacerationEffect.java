package Alone818.com.alone_journey.effects;

import Alone818.com.alone_journey.init.ModEffects;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 撕裂：负面效果。每次命中叠加 1 级（amplifier +1），时长固定为 3 秒（60 tick）。
 * 持续期间不造成伤害，3 秒到期时结算一次等同于等级数值的伤害
 * （撕裂 I = 1 点，撕裂 IV = 4 点），随后等级减半继续下一轮
 * （撕裂 IV 结算 4 点 -> 撕裂 II -> 撕裂 I -> 消失）。
 */
public class LacerationEffect extends MobEffect {

    // 标准持续时长（3 秒），到期时结算一次伤害
    public static final int SETTLE_DURATION_TICKS = 30;

    // 撕裂结算伤害的保底比例：护甲减免后实际伤害不低于原始伤害的 60%
    public static final float MIN_DAMAGE_RATIO = 0.6F;

    // 到期重施加登记：实体 UUID -> 减半后的等级(amplifier)。
    // 不能在 applyEffectTick 里直接移除/重加效果（tickEffects 正在遍历效果表，会并发修改），
    // 由 LacerationEvent 在实体 tick 时处理。
    private static final Map<UUID, Integer> PENDING_REAPPLY = new HashMap<>();

    public LacerationEffect() {
        // 负面效果分类，液体颜色为暗红色
        super(MobEffectCategory.HARMFUL, 0x8B0000);
    }

    // 只在到期前最后一刻（duration == 1）触发一次结算
    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration == 1;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // 结算一次等同于等级数值的伤害（撕裂 I = 1 点，撕裂 IV = 4 点）：
        // 视为物理伤害，按原版护甲公式减免，但实际伤害不低于原始伤害的 60%
        int level = amplifier + 1;
        entity.hurt(entity.damageSources().magic(), getSettleDamage(entity, level));

        // 登记等级减半后的下一轮（减半为 0 则效果自然消失，无需登记）
        int newLevel = level / 2;
        if (newLevel >= 1) {
            PENDING_REAPPLY.put(entity.getUUID(), newLevel - 1);
        }
    }

    /**
     * 计算撕裂结算的最终伤害：先按原版护甲公式（含韧性）算出减免后数值，
     * 再与 60% 保底值取较大者。结果用免护甲伤害源直接结算，避免 vanilla 二次减免。
     */
    private static float getSettleDamage(LivingEntity entity, float raw) {
        float armor = entity.getArmorValue();
        float toughness = (float) entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float afterArmor = CombatRules.getDamageAfterAbsorb(raw, armor, toughness);
        return Math.max(afterArmor, raw * MIN_DAMAGE_RATIO);
    }

    /**
     * 供 LacerationEvent 在实体 tick（效果表遍历之外）调用：
     * 若上一轮撕裂已到期且实体身上没有新的撕裂，按减半等级重新施加固定 3 秒。
     */
    public static void tryReapply(LivingEntity entity) {
        Integer amplifier = PENDING_REAPPLY.remove(entity.getUUID());
        if (amplifier != null && entity.isAlive() && !entity.hasEffect(ModEffects.LACERATION.get())) {
            entity.addEffect(new MobEffectInstance(
                    ModEffects.LACERATION.get(), SETTLE_DURATION_TICKS, amplifier));
        }
    }
}
