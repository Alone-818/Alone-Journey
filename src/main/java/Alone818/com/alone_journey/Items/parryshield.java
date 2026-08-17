package Alone818.com.alone_journey.Items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class parryshield extends Item {

    // 配置常量
    public static final int PARRY_WINDOW_TICKS = 20;        // 招架窗口：举盾小于此 tick 数（1 秒）内受击判定为招架
    public static final float LONG_BLOCK_ABSORB = 0.8f;     // 长时间举盾时吸收的伤害比例（80%）
    public static final float PARRY_REFLECT_RATIO = 0.7f;   // 招架成功时反弹给攻击者的伤害比例（70%）
    public static final int WEAKNESS_DURATION_TICKS = 60;   // 招架成功后给予攻击者的虚弱时长（3 秒）
    public static final int WEAKNESS_AMPLIFIER = 0;         // 虚弱等级（0 = 虚弱 I）
    public static final int PARRY_COOLDOWN_TICKS = 20;      // 盾反冷却：招架成功后此 tick 数（1 秒）内无法再次招架

    public parryshield() {
        super(new Properties().durability(512));
    }

    // 以下三个方法让物品像原版盾牌一样右键举起格挡
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    // 支持盾牌工具动作，确保原版 isBlocking / isDamageSourceBlocked 判定通过
    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        return ToolActions.SHIELD_BLOCK == toolAction;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.alone_journey.parryshield.tooltip.desc")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.alone_journey.parryshield.tooltip.parry_desc")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.alone_journey.parryshield.tooltip.block_desc")
                .withStyle(ChatFormatting.GRAY));
    }
}
