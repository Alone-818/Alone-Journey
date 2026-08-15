package Alone818.com.alone_journey.events;

import Alone818.com.alone_journey.Itemcuiros.crystalline_heart;
import Alone818.com.alone_journey.init.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.util.ICuriosHelper;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = Alone818.com.alone_journey.Alone_journey.MODID)
public class ShieldEvent {
    // 回复冷却时间间隔（以tick为单位，1秒=20 tick。例如 10 秒 = 200 tick）
    private static final int HEAL_INTERVAL_TICKS = 200;

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();

        // 只对玩家生效护盾
        if (!(entity instanceof Player player)) {
            return;
        }

        // 如果玩家已经拥有抗性 5（强度等级为 4）及以上的效果，跳过护盾触发，或者普通伤害直接被抗性免疫
        // 主要是防多次同时触发或冲突
        if (player.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
            MobEffectInstance effect = player.getEffect(MobEffects.DAMAGE_RESISTANCE);
            if (effect != null && effect.getAmplifier() >= 4) {
                // 拥有抗性 V 级以上已经可以免除所有伤害，直接跳过护盾扣除
                return;
            }
        }
        ICuriosHelper helper = CuriosApi.getCuriosHelper();
        Optional<SlotResult> crystalOpt = helper.findFirstCurio(player, ModItems.CRYSTALLINE_HEART.get());
        if (crystalOpt.isEmpty()) {
            return;
        }

        SlotResult slotResult = crystalOpt.get();
        ItemStack stack = slotResult.stack();
        CompoundTag tag = stack.getOrCreateTag();

        double currentShield = tag.getDouble(crystalline_heart.NB_TAG_SHIELD);

        if (currentShield <= 0) {
            return;
        }

        float damage = event.getAmount();
        if (damage <= 0) {
            return;
        }

        // 扣除 1 点护盾
        double newShield = currentShield - 1;
        tag.putDouble(crystalline_heart.NB_TAG_SHIELD, newShield);

        // 伤害发生时重置/刷新上次回复时间，防止受伤后立即回盾，或者保持原来的逻辑
        // 我们选择不清除它，或者把它设置为当前时间以开始下一次回复周期的倒计时
        tag.putLong(crystalline_heart.NB_TAG_LAST_HEAL, player.level().getGameTime());

        stack.setTag(tag);

        // 写回真实槽位
        String slotTypeId = slotResult.slotContext().identifier();
        int index = slotResult.slotContext().index();
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            handler.setEquippedCurio(slotTypeId, index, stack);
        });

        // 给予玩家 2 秒 (40 tick) 的 抗性 V 效果 (amplifier = 4)
        // 40 tick, amplifier 4 (即抗性 5), 两个 boolean 参数分别表示是否是环境效果、是否显示粒子和图标
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 4, false, true, true));

        // 免疫此次伤害
        event.setCanceled(true);

    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 只在服务器端且在Tick的END阶段进行计算，避免双倍运行
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }

        Player player = event.player;
        ICuriosHelper helper = CuriosApi.getCuriosHelper();
        Optional<SlotResult> crystalOpt = helper.findFirstCurio(player, ModItems.CRYSTALLINE_HEART.get());
        if (crystalOpt.isEmpty()) {
            return;
        }
        double playerArmor =player.getArmorValue();
        double playerToughness=player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        SlotResult slotResult = crystalOpt.get();
        ItemStack stack = slotResult.stack();
        CompoundTag tag = stack.getOrCreateTag();

        double maxShield = tag.getDouble(crystalline_heart.NB_TAG_MAX_SHIELD)+playerArmor/4;
        double currentShield = tag.getDouble(crystalline_heart.NB_TAG_SHIELD);

        // 如果当前护盾已经满了，重置计时器为当前时间并返回
        if (currentShield >= maxShield) {
            tag.putLong(crystalline_heart.NB_TAG_LAST_HEAL, player.level().getGameTime());
            return;
        }

        long gameTime = player.level().getGameTime();
        if (!tag.contains(crystalline_heart.NB_TAG_LAST_HEAL)) {
            // 如果不存在上次回复时间，初始化为当前时间
            tag.putLong(crystalline_heart.NB_TAG_LAST_HEAL, gameTime);
            stack.setTag(tag);

            // 写回槽位
            String slotTypeId = slotResult.slotContext().identifier();
            int index = slotResult.slotContext().index();
            CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                handler.setEquippedCurio(slotTypeId, index, stack);
            });
            return;
        }

        long lastHealTime = tag.getLong(crystalline_heart.NB_TAG_LAST_HEAL);
        long elapsedTicks = gameTime - lastHealTime;

        if (elapsedTicks >= HEAL_INTERVAL_TICKS-playerToughness*8) {
            // 经过 HEAL_INTERVAL_TICKS 后回复 1 点护盾
            double newShield = Math.min(maxShield, currentShield + 1);
            tag.putDouble(crystalline_heart.NB_TAG_SHIELD, newShield);
            // 更新回复时间
            tag.putLong(crystalline_heart.NB_TAG_LAST_HEAL, gameTime);
            stack.setTag(tag);

            // 写回槽位
            String slotTypeId = slotResult.slotContext().identifier();
            int index = slotResult.slotContext().index();
            CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                handler.setEquippedCurio(slotTypeId, index, stack);
            });
        }
    }
}
