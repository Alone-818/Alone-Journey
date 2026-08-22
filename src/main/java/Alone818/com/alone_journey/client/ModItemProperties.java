package Alone818.com.alone_journey.client;

import Alone818.com.alone_journey.Alone_journey;
import Alone818.com.alone_journey.init.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 客户端物品属性注册：为招架之盾注册 blocking 属性（举盾格挡时为 1）。
 * 模型 overrides 据此在举盾时切换到 parryshield_blocking 模型：
 * 盾面旋转 90° 朝向身前，第一人称呈现类似原版盾牌的架盾画面。
 */
@Mod.EventBusSubscriber(modid = Alone_journey.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModItemProperties {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                ModItems.PARRY_SHIELD.get(),
                new ResourceLocation(Alone_journey.MODID, "blocking"),
                (stack, level, entity, seed) ->
                        entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F));
    }
}
