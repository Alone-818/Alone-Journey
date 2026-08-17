package Alone818.com.alone_journey.Items;

import Alone818.com.alone_journey.init.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class chainsword extends SwordItem {

    // 撕裂：命中后叠加等级（每次+1），时长固定为3秒（60 tick）。
    // 撕裂3秒到期结算一次等同于等级数值的伤害。
    public static final int LACERATION_DURATION_TICKS =60;

    public chainsword() {
        // 伤害修饰 5 + 钻石 3 = 8（总攻击力 9，即钻石剑 +2）；攻速 -2.8（钻石剑为 -2.4）
        super(Tiers.DIAMOND, 5, -2.8F, new Properties());
    }

    /**
     * 命中实体时叠加撕裂等级（每次+1），持续时长固定为3秒。
     * 撕裂3秒后结算一次等同于最终等级的伤害。
     */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        int newAmplifier = 0;
        MobEffectInstance existing = target.getEffect(ModEffects.LACERATION.get());
        if (existing != null) {
            // 在现有等级基础上+1
            newAmplifier = existing.getAmplifier() + 2;
        }
        target.addEffect(new MobEffectInstance(
                ModEffects.LACERATION.get(),
                LACERATION_DURATION_TICKS,
                newAmplifier));
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.alone_journey.chainsword.tooltip.desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.alone_journey.chainsword.tooltip.laceration_desc")
                .withStyle(ChatFormatting.RED));
    }
}