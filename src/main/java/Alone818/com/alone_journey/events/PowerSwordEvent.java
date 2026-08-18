package Alone818.com.alone_journey.events;

import Alone818.com.alone_journey.Alone_journey;
import Alone818.com.alone_journey.Items.powersword;
import Alone818.com.alone_journey.init.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Alone_journey.MODID)
public class PowerSwordEvent {

    /**
     * 超频计时：每 tick 递减剩余时长，归零时移除标记与伤害修饰符；
     * 期间持续补回伤害修饰符（防止维度切换/重登录导致 transient 修饰符丢失）。
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(powersword.OVERCLOCK_TAG)) {
            return;
        }
        powersword.applyOverclockDamage(entity);
        int ticks = data.getInt(powersword.OVERCLOCK_TAG) - 1;
        if (ticks <= 0) {
            powersword.removeOverclock(entity);
        } else {
            data.putInt(powersword.OVERCLOCK_TAG, ticks);
        }
    }

    /**
     * 动力剑主攻击结算（LivingHurtEvent 在护甲减免之前触发）：
     * 1. 无视韧性：按原版护甲公式预补偿韧性带来的额外减伤，
     *    使最终落地伤害等同于目标韧性为 0 时的结果。
     * 2. 超频吸血：持有者处于超频模式时，本次伤害的 5/0% 转化为治疗。
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getDirectEntity() instanceof LivingEntity attacker)) {
            return;
        }
        ItemStack weapon = attacker.getMainHandItem();
        if (!weapon.is(ModItems.POWERSWORD.get())) {
            return;
        }

        LivingEntity target = event.getEntity();

        // 无视韧性（魔法穿甲等已无视护甲的伤害源无需补偿）
        if (!source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            float amount = event.getAmount();
            float armor = target.getArmorValue();
            float toughness = (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);

            // 原版护甲减伤：g = clamp(armor - damage / (2 + toughness / 4), armor * 0.2, 20)
            float withToughness = Mth.clamp(armor - amount / (2.0F + toughness / 4.0F), armor * 0.2F, 20.0F);
            float noToughness = Mth.clamp(armor - amount / 2.0F, armor * 0.2F, 20.0F);

            if (withToughness > noToughness) {
                // 反解出需要的入伤量，使 (新伤害) * (1 - 有韧性减免/25) = 原伤害 * (1 - 无韧性减免/25)
                float bonus = amount * (withToughness - noToughness) / (25.0F - withToughness);
                event.setAmount(amount + bonus);
            }
        }

        // 超频吸血：主攻击与穿甲伤害均参与
        if (attacker instanceof Player player && powersword.hasOverclock(player)) {
            player.heal(event.getAmount() * powersword.LIFESTEAL_RATIO);
        }
    }
}
