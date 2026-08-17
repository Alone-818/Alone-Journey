package Alone818.com.alone_journey.events;

import Alone818.com.alone_journey.effects.LacerationEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Alone818.com.alone_journey.Alone_journey.MODID)
public class LacerationEvent {

    /**
     * 撕裂到期结算后的等级减半续期：在实体 tick（效果表遍历之外）安全地重新施加，
     * 避免在 applyEffectTick 内修改效果表导致并发修改异常。
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.level().isClientSide()) {
            LacerationEffect.tryReapply(entity);
        }
    }
}
