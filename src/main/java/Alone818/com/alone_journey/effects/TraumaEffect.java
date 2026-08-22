package Alone818.com.alone_journey.effects;

import Alone818.com.alone_journey.Config;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 创伤：负面效果。持有期间每 0.5 秒（10 tick）结算一次魔法伤害，
 * 每层 0.2 点（可在配置中调整）；同时每层使受到的所有伤害提高 1%
 * （增伤部分由 TraumaEvent 在 LivingHurtEvent 中处理）。
 */
public class TraumaEffect extends MobEffect {

    // 结算间隔：0.5 秒 = 10 tick
    public static final int TICK_INTERVAL = 10;

    public TraumaEffect() {
        // 负面效果分类，液体颜色为暗红色
        super(MobEffectCategory.HARMFUL, 0xA0522D);
    }

    // 固定每 10 tick 结算一次（区别于原版 25 >> amplifier 的随等级衰减触发）
    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % TICK_INTERVAL == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // 每层 0.2 点魔法伤害（创伤 I = 0.2/次，创伤 V = 1.0/次）
        int level = amplifier + 1;
        entity.hurt(entity.damageSources().magic(),
                (float) (level * Config.traumaMagicDamagePerLevel));
    }
}
