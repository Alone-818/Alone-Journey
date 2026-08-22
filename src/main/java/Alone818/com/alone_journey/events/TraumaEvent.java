package Alone818.com.alone_journey.events;

import Alone818.com.alone_journey.Alone_journey;
import Alone818.com.alone_journey.Config;
import Alone818.com.alone_journey.init.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 创伤事件：处理增伤与施加/叠层。
 * 每 1 层创伤使实体受到的所有伤害提高 1%（默认，可在配置中调整）；
 * applyOrStack 供武器等外部逻辑施加创伤，重复施加叠加 1 层并刷新持续时间。
 */
@Mod.EventBusSubscriber(modid = Alone_journey.MODID)
public class TraumaEvent {

    /** 每层创伤提高受到的伤害比例（默认每层 +1%） */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        MobEffectInstance trauma = target.getEffect(ModEffects.TRAUMA.get());
        if (trauma != null) {
            int level = trauma.getAmplifier() + 1;
            float bonus = (float) (level * Config.traumaDamageTakenPerLevel);
            event.setAmount(event.getAmount() * (1.0F + bonus));
        }
    }

    /**
     * 施加或叠加指定层数的创伤（层数受配置最大层数限制），
     * 重复施加刷新为配置的默认持续时间。
     */
    public static void applyOrStack(LivingEntity target, int layers) {
        if (target.level().isClientSide()) {
            return;
        }
        if (layers <= 0) {
            return;
        }
        MobEffectInstance current = target.getEffect(ModEffects.TRAUMA.get());
        int newAmplifier = current == null
                ? layers - 1
                : Math.min(current.getAmplifier() + layers, Config.traumaMaxLayers - 1);
        target.addEffect(new MobEffectInstance(
                ModEffects.TRAUMA.get(), Config.traumaBaseDuration, newAmplifier));
    }
}
