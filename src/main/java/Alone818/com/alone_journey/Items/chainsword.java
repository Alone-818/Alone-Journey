package Alone818.com.alone_journey.Items;

import Alone818.com.alone_journey.Config;
import Alone818.com.alone_journey.init.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ToolAction;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class chainsword extends SwordItem {

    // 撕裂：命中后叠加等级（每次+1），时长固定为3秒（60 tick）。
    // 撕裂3秒到期结算一次等同于等级数值的伤害。
    public static final int LACERATION_DURATION_TICKS = 20;

    // 长按振砍：每 N tick 挥砍一次
    public static final int SLASH_INTERVAL_TICKS = 2;
    // 长按振砍每次伤害值（4.0 = 2 心）
    public static final float SLASH_DAMAGE = 1F;
    // 长按时长限制（秒）：3秒 = 60 tick（默认值，实际以配置 chainsword.overclockDuration 为准）
    public static final int MIN_USE_FOR_COOLDOWN_TICKS = 10;
    // 长按振砍扇形角度（90°）
    public static final float SLASH_ARC_DEGREES = 90.0F;

    // 长按振砍有效范围（半径格数）
    public static final float SLASH_RANGE = 4.0F;

    public chainsword() {
        // 伤害修饰 5 + 钻石 3 = 8（总攻击力 9，即钻石剑 +2）；攻速 -2.8（钻石剑为 -2.4）
        super(Tiers.DIAMOND, 5, -2.8F, new Properties().durability(880));
    }

    /**
     * 长按右键触发振砍，持续按住可不断对面前扇形范围内的敌人造成低伤并叠加撕裂。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    /**
     * 长按时长限制（由配置 chainsword.overclockDuration 决定，默认 60 tick = 3 秒）
     */
    @Override
    public int getUseDuration(ItemStack stack) {
        return Config.chainswordOverclockDuration;
    }

    /**
     * 松开右键（或切换物品打断）结束超频：引导达到最短时长则进入技能冷却
     */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        applySkillCooldown(entity, getUseDuration(stack) - timeLeft);
    }

    /**
     * 引导满技能时长自然结束：完整使用，进入技能冷却
     */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        applySkillCooldown(entity, getUseDuration(stack));
        return stack;
    }

    /**
     * 使用原版物品冷却机制：冷却期间右键被屏蔽，物品栏上会显示冷却遮罩。
     */
    private void applySkillCooldown(LivingEntity entity, int heldTicks) {
        if (heldTicks < MIN_USE_FOR_COOLDOWN_TICKS) {
            return;
        }
        if (entity instanceof net.minecraft.world.entity.player.Player player && !player.level().isClientSide()) {
            player.getCooldowns().addCooldown(this, Config.chainswordOverclockCooldown);
        }
    }

    /**
     * 长按过程中按 SLASH_INTERVAL_TICKS 间隔对前方扇形区域内的敌人挥砍。
     */
    @Override
    public void onUseTick(Level pLevel, LivingEntity entity, ItemStack stack, int pRemainingUseDuration) {

        if (entity.level().isClientSide) return;
        if (!(entity instanceof net.minecraft.world.entity.player.Player player)) return;
        // 按 tick 间隔挥砍，避免过快
        if (entity.tickCount % SLASH_INTERVAL_TICKS != 0) return;

        Vec3 eyePos = entity.position().add(0.0D, entity.getEyeHeight(), 0.0D);
        Vec3 lookDir = entity.getLookAngle();

        List<LivingEntity> candidates = entity.level().getEntitiesOfClass(
                LivingEntity.class,
                new AABB(
                        eyePos.x - SLASH_RANGE, eyePos.y - SLASH_RANGE, eyePos.z - SLASH_RANGE,
                        eyePos.x + SLASH_RANGE, eyePos.y + SLASH_RANGE, eyePos.z + SLASH_RANGE
                )
        );

        for (LivingEntity target : candidates) {
            if (target == entity) continue;
            if (target.isAlliedTo(entity)) continue;

            Vec3 toTarget = target.position().add(0.0D, target.getEyeHeight(), 0.0D).subtract(eyePos).normalize();
            double cosHalfArc = Math.cos(Math.toRadians(SLASH_ARC_DEGREES / 2.0F));
            if (lookDir.normalize().dot(toTarget) < cosHalfArc) continue;

            // 造成低伤（无视无敌帧）
            target.invulnerableTime = 0;
            target.hurt(entity.damageSources().playerAttack(player), SLASH_DAMAGE);
            // 取消击退
            target.setDeltaMovement(0.0D, target.getDeltaMovement().y, 0.0D);
            // 叠加撕裂
            this.hurtEnemy(stack, target, entity);
            // 损耗耐久
            stack.hurtAndBreak(1, player, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        }
    }

    /**
     * 对应史蒂夫的物品标签页，使剑在副手也能使用本工具动作（当前无额外额外动作，仅作为占位方便扩展）。
     */
    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        return false;
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
            newAmplifier = existing.getAmplifier() + 1;
        }
        target.addEffect(new MobEffectInstance(
                ModEffects.LACERATION.get(),
                LACERATION_DURATION_TICKS,
                newAmplifier));
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_journey.chainsword.tooltip.desc")
                    .withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.translatable("item.alone_journey.chainsword.tooltip.laceration_desc")
                    .withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable("item.alone_journey.chainsword.tooltip.desc")
                    .withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.translatable("tooltip.alone_journey.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}