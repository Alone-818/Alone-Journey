package Alone818.com.alone_journey.events;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 玩家手术效果事件处理类
 * 负责管理玩家的手术效果，包括护甲、伤害、速度等
 */
@Mod.EventBusSubscriber(modid = "alone_journey", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SurgeryEffectEvent {

    /**
     * 存储玩家UUID->三个手术类型各自的等级
     */
    private static final Map<UUID, int[]> playerSurgeryData = new HashMap<>();

    /**
     * 存储玩家是否已获得首次手术免疫（从NBT恢复）
     */
    private static final Map<UUID, Boolean> playerFirstImmunity = new HashMap<>();

    /**
     * 首次手术类型索引
     * 0=护甲术(护甲+2，韧性+1)
     * 1=伤害术(攻击伤害+10%，攻击速度+15%)
     * 2=速度术(移动速度+10%，跳跃高度+15%，水下移动速度+20%，安全摔落高度+20%)
     * 3=生命术(生命上限+4，额外氧气+30%)
     * 4=幸运术(幸运+2)
     */
    public static final int SURGERY_ARMOR = 0;
    public static final int SURGERY_DAMAGE = 1;
    public static final int SURGERY_SPEED = 2;
    public static final int SURGERY_HEALTH = 3;
    public static final int SURGERY_LUCK = 4;

    // 手术台等级对应的玩家最大手术等级上限
    // 手术台1级→4级上限, 2级→8级上限, 3级→12级上限, 4级→16级上限, 5级→21级上限
    private static final int[] TABLE_LEVEL_TO_MAX_LEVEL = {0, 4, 8, 12, 16, 21};

    // 药水效果免疫列表（首次手术时获得）
    // 反胃在 Minecraft 中为 CONFUSION，不是 HUNGER（饥饿）
    private static final net.minecraft.world.effect.MobEffect[] IMMUNITY_EFFECTS = {
            MobEffects.BLINDNESS,   // 失明
            MobEffects.POISON,      // 剧毒
            MobEffects.CONFUSION,   // 反胃
            MobEffects.DARKNESS     // 黑暗
    };

    // 属性修饰符UUID
    private static final UUID ARMOR_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TOUGHNESS_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID DAMAGE_UUID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ATTACK_SPEED_UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID SPEED_UUID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID JUMP_UUID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID HEALTH_UUID = UUID.fromString("88888888-8888-8888-8888-888888888888");
    private static final UUID LUCK_UUID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    /**
     * 每级提升数值
     * 护甲: +2 护甲值, +1 护甲韧性
     * 伤害: +10% 攻击伤害, +15% 攻击速度
     * 速度: +10% 移动速度, +15% 跳跃高度,
     * 生命: +4 生命上限,
     * 幸运: +2 幸运
     */
    private static final double ARMOR_PER_LEVEL = 2.0;
    private static final double TOUGHNESS_PER_LEVEL = 1.0;
    private static final double DAMAGE_PER_LEVEL = 0.05;
    private static final double ATTACK_SPEED_PER_LEVEL = 0.15;
    private static final double SPEED_PER_LEVEL = 0.10;
    private static final double JUMP_PER_LEVEL = 0.15;
    private static final double HEALTH_PER_LEVEL = 4.0;
    private static final double LUCK_PER_LEVEL = 2.0;

    /**
     * 根据手术台等级获取玩家手术等级上限
     * 手术台1级→玩家手术上限4级
     * 手术台2级→玩家手术上限8级
     * 手术台3级→玩家手术上限12级
     * 手术台4级→玩家手术上限16级
     * 手术台5级→玩家手术上限21级
     */
    public static int getMaxLevelByTableLevel(int tableLevel) {
        if (tableLevel < 1) tableLevel = 1;
        if (tableLevel > 5) tableLevel = 5;
        return TABLE_LEVEL_TO_MAX_LEVEL[tableLevel];
    }

    /**
     * 检查物品是否为护甲术手术材料（铁锭）
     */
    public static boolean isArmorSurgeryMaterial(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(Items.IRON_INGOT);
    }

    /**
     * 检查物品是否为伤害术手术材料（烈焰棒）
     */
    public static boolean isDamageSurgeryMaterial(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(Items.BLAZE_ROD);
    }

    /**
     * 检查物品是否为速度术手术材料（末影水晶）
     */
    public static boolean isSpeedSurgeryMaterial(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(Items.END_CRYSTAL);
    }

    /**
     * 检查物品是否为生命术手术材料（金苹果）
     */
    public static boolean isHealthSurgeryMaterial(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(Items.GOLDEN_APPLE);
    }

    /**
     * 检查物品是否为幸运术手术材料（紫水晶碎片）
     */
    public static boolean isLuckSurgeryMaterial(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(Items.AMETHYST_SHARD);
    }

    /**
     * 根据手术材料获取手术类型
     * @return 0=护甲, 1=伤害, 2=速度, 3=生命, 4=幸运, -1=无效材料
     */
    public static int getSurgeryTypeFromMaterial(ItemStack stack) {
        if (isArmorSurgeryMaterial(stack)) return SURGERY_ARMOR;
        if (isDamageSurgeryMaterial(stack)) return SURGERY_DAMAGE;
        if (isSpeedSurgeryMaterial(stack)) return SURGERY_SPEED;
        if (isHealthSurgeryMaterial(stack)) return SURGERY_HEALTH;
        if (isLuckSurgeryMaterial(stack)) return SURGERY_LUCK;
        return -1;
    }

    /**
     * 获取手术类型的描述名称
     */
    public static String getSurgeryTypeName(int type) {
        return switch (type) {
            case SURGERY_ARMOR -> "护甲";
            case SURGERY_DAMAGE -> "伤害";
            case SURGERY_SPEED -> "速度";
            case SURGERY_HEALTH -> "生命";
            case SURGERY_LUCK -> "幸运";
            default -> "未知";
        };
    }

    /**
     * 获取玩家的总手术等级（五种类型等级相加）
     */
    public static int getTotalSurgeryLevel(Player player) {
        UUID playerId = player.getUUID();
        int[] data = playerSurgeryData.get(playerId);
        if (data == null) {
            return 0;
        }
        return data[SURGERY_ARMOR] + data[SURGERY_DAMAGE] + data[SURGERY_SPEED] + data[SURGERY_HEALTH] + data[SURGERY_LUCK];
    }

    /**
     * 添加一次手术
     *
     * @param player   进行手术的玩家
     * @param surgeryType 手术类型: 0(护甲)、1(伤害)、2(速度)、3(生命)、4(幸运)
     * @param tableLevel 手术台当前等级
     * @return 手术是否成功
     */
    public static boolean applySurgery(Player player, int surgeryType, int tableLevel) {
        if (surgeryType < 0 || surgeryType > 4) {
            return false;
        }

        UUID playerId = player.getUUID();
        int[] data = playerSurgeryData.computeIfAbsent(playerId, k -> new int[5]);

        int maxLevel = getMaxLevelByTableLevel(tableLevel);
        int currentTotalLevel = data[SURGERY_ARMOR] + data[SURGERY_DAMAGE] + data[SURGERY_SPEED] + data[SURGERY_HEALTH] + data[SURGERY_LUCK];

        if (currentTotalLevel >= maxLevel) {
            return false; // 总手术等级已满级
        }

        // 首次手术免疫
        if (!playerFirstImmunity.getOrDefault(playerId, false)) {
            applyFirstImmunity(player);
            playerFirstImmunity.put(playerId, true);
        }

        // 提升手术等级
        data[surgeryType]++;

        // 保存手术等级到NBT（持久化）
        CompoundTag nbt = player.getPersistentData();
        saveSurgeryLevelsToNbt(player, data);

        // 刷新属性
        refreshAttributes(player);

        return true;
    }

    /**
     * 根据材料应用手术效果
     */
    public static boolean performSurgery(Player player, ItemStack material, int tableLevel) {
        int surgeryType = getSurgeryTypeFromMaterial(material);
        if (surgeryType < 0) {
            return false;
        }

        return applySurgery(player, surgeryType, tableLevel);
    }

    /**
     * 将手术等级数据保存到NBT
     */
    private static void saveSurgeryLevelsToNbt(Player player, int[] data) {
        CompoundTag nbt = player.getPersistentData();
        nbt.putInt("SurgeryArmorLevel", data[SURGERY_ARMOR]);
        nbt.putInt("SurgeryDamageLevel", data[SURGERY_DAMAGE]);
        nbt.putInt("SurgerySpeedLevel", data[SURGERY_SPEED]);
        nbt.putInt("SurgeryHealthLevel", data[SURGERY_HEALTH]);
        nbt.putInt("SurgeryLuckLevel", data[SURGERY_LUCK]);
    }

    /**
     * 从NBT恢复手术等级数据
     */
    private static int[] loadSurgeryLevelsFromNbt(Player player) {
        CompoundTag nbt = player.getPersistentData();
        int[] data = new int[5];
        data[SURGERY_ARMOR] = nbt.getInt("SurgeryArmorLevel");
        data[SURGERY_DAMAGE] = nbt.getInt("SurgeryDamageLevel");
        data[SURGERY_SPEED] = nbt.getInt("SurgerySpeedLevel");
        data[SURGERY_HEALTH] = nbt.getInt("SurgeryHealthLevel");
        data[SURGERY_LUCK] = nbt.getInt("SurgeryLuckLevel");
        return data;
    }

    public static int stringToSurgeryType(String type) {
        return switch (type) {
            case "armor" -> SURGERY_ARMOR;
            case "damage" -> SURGERY_DAMAGE;
            case "speed" -> SURGERY_SPEED;
            default -> -1;
        };
    }

    public static String surgeryTypeToString(int type) {
        return switch (type) {
            case SURGERY_ARMOR -> "armor";
            case SURGERY_DAMAGE -> "damage";
            case SURGERY_SPEED -> "speed";
            default -> "unknown";
        };
    }

    private static void applyFirstImmunity(Player player) {
        CompoundTag nbt = player.getPersistentData();
        nbt.putBoolean("SurgeryFirstImmunity", true);
        // 添加尺寸放大效果（Scale = 0.5，表示放大 1.5 倍）
        nbt.putDouble("SurgeryScale", 0.5);
        playerFirstImmunity.put(player.getUUID(), true);
        // 清除已有的负面效果
        for (net.minecraft.world.effect.MobEffect effect : IMMUNITY_EFFECTS) {
            player.removeEffect(effect);
        }
    }

    /**
     * 重新计算并设置玩家属性修饰符
     */
    public static void refreshAttributes(Player player) {
        UUID playerId = player.getUUID();
        int[] data = playerSurgeryData.get(playerId);
        if (data == null) {
            data = new int[5];
            playerSurgeryData.put(playerId, data);
        }

        // 护甲与韧性
        double armorBonus = data[SURGERY_ARMOR] * ARMOR_PER_LEVEL;
        double toughnessBonus = data[SURGERY_ARMOR] * TOUGHNESS_PER_LEVEL;

        AttributeInstance armorAttr = player.getAttribute(Attributes.ARMOR);
        AttributeInstance toughnessAttr = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (armorAttr != null) {
            if (armorAttr.getModifier(ARMOR_UUID) != null) {
                armorAttr.removeModifier(ARMOR_UUID);
            }
            if (armorBonus > 0) {
                armorAttr.addPermanentModifier(new AttributeModifier(
                        ARMOR_UUID, "Surgery Armor", armorBonus, AttributeModifier.Operation.ADDITION));
            }
        }
        if (toughnessAttr != null) {
            if (toughnessAttr.getModifier(TOUGHNESS_UUID) != null) {
                toughnessAttr.removeModifier(TOUGHNESS_UUID);
            }
            if (toughnessBonus > 0) {
                toughnessAttr.addPermanentModifier(new AttributeModifier(
                        TOUGHNESS_UUID, "Surgery Toughness", toughnessBonus, AttributeModifier.Operation.ADDITION));
            }
        }

        // 伤害加成 (乘法修饰符)
        double damageBonus = data[SURGERY_DAMAGE] * DAMAGE_PER_LEVEL;
        AttributeInstance damageAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damageAttr != null) {
            if (damageAttr.getModifier(DAMAGE_UUID) != null) {
                damageAttr.removeModifier(DAMAGE_UUID);
            }
            if (damageBonus > 0) {
                damageAttr.addPermanentModifier(new AttributeModifier(
                        DAMAGE_UUID, "Surgery Damage", damageBonus, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }

        // 攻击速度
        double attackSpeedBonus = data[SURGERY_DAMAGE] * ATTACK_SPEED_PER_LEVEL;
        AttributeInstance attackSpeedAttr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeedAttr != null) {
            if (attackSpeedAttr.getModifier(ATTACK_SPEED_UUID) != null) {
                attackSpeedAttr.removeModifier(ATTACK_SPEED_UUID);
            }
            if (attackSpeedBonus > 0) {
                attackSpeedAttr.addPermanentModifier(new AttributeModifier(
                        ATTACK_SPEED_UUID, "Surgery Attack Speed", attackSpeedBonus, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }

        // 速度
        double speedBonus = data[SURGERY_SPEED] * SPEED_PER_LEVEL;
        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            if (speedAttr.getModifier(SPEED_UUID) != null) {
                speedAttr.removeModifier(SPEED_UUID);
            }
            if (speedBonus > 0) {
                speedAttr.addPermanentModifier(new AttributeModifier(
                        SPEED_UUID, "Surgery Speed", speedBonus, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }

        // 跳跃高度
        double jumpBonus = data[SURGERY_SPEED] * JUMP_PER_LEVEL;
        AttributeInstance jumpAttr = player.getAttribute(Attributes.JUMP_STRENGTH);
        if (jumpAttr != null) {
            if (jumpAttr.getModifier(JUMP_UUID) != null) {
                jumpAttr.removeModifier(JUMP_UUID);
            }
            if (jumpBonus > 0) {
                jumpAttr.addPermanentModifier(new AttributeModifier(
                        JUMP_UUID, "Surgery Jump", jumpBonus, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }

        // 生命上限
        double healthBonus = data[SURGERY_HEALTH] * HEALTH_PER_LEVEL;
        AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr != null) {
            if (maxHealthAttr.getModifier(HEALTH_UUID) != null) {
                maxHealthAttr.removeModifier(HEALTH_UUID);
            }
            if (healthBonus > 0) {
                maxHealthAttr.addPermanentModifier(new AttributeModifier(
                        HEALTH_UUID, "Surgery Health", healthBonus, AttributeModifier.Operation.ADDITION));
            }
        }
        // 幸运
        double luckBonus = data[SURGERY_LUCK] * LUCK_PER_LEVEL;
        AttributeInstance luckAttr = player.getAttribute(Attributes.LUCK);
        if (luckAttr != null) {
            if (luckAttr.getModifier(LUCK_UUID) != null) {
                luckAttr.removeModifier(LUCK_UUID);
            }
            if (luckBonus > 0) {
                luckAttr.addPermanentModifier(new AttributeModifier(
                        LUCK_UUID, "Surgery Luck", luckBonus, AttributeModifier.Operation.ADDITION));
            }
        }
    }

    /**
     * 获取玩家的手术效果等级
     */
    public static int getSurgeryLevel(Player player, int surgeryType) {
        UUID playerId = player.getUUID();
        int[] data = playerSurgeryData.get(playerId);
        if (data == null) {
            return 0;
        }
        if (surgeryType < 0 || surgeryType > 4) {
            return 0;
        }
        return data[surgeryType];
    }

    /**
     * 检查玩家是否拥有指定类型的手术提升
     */
    public static boolean hasSurgeryLevel(Player player, int surgeryType) {
        UUID playerId = player.getUUID();
        int[] data = playerSurgeryData.get(playerId);
        if (data == null) {
            return false;
        }
        return data[surgeryType] > 0;
    }

    /**
     * 玩家受伤时应用伤害加成
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        UUID playerId = player.getUUID();
        int[] data = playerSurgeryData.get(playerId);
        if (data == null || data[SURGERY_DAMAGE] <= 0) {
            return;
        }

        // 每级增加5%伤害（这里指玩家造成的伤害，应在攻击时处理）
        // 若需要在受伤时也处理可取消
    }

    /**
     * 玩家攻击时增加伤害（在LivingHurtEvent中处理）
     */
    @SubscribeEvent
    public static void onPlayerAttack(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof Player player)) {
            return;
        }

        UUID playerId = player.getUUID();
        int[] data = playerSurgeryData.get(playerId);
        if (data == null || data[SURGERY_DAMAGE] <= 0) {
            return;
        }

        float multiplier = 1.0f + (float) (data[SURGERY_DAMAGE] * DAMAGE_PER_LEVEL);
        event.setAmount(event.getAmount() * multiplier);
    }

    /**
     * 阻止免疫的药水效果应用到玩家身上
     */
    @SubscribeEvent
    public static void onEffectApplicable(net.minecraftforge.event.entity.living.MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof Player player) {
            if (hasFirstImmunity(player)) {
                net.minecraft.world.effect.MobEffect effect = event.getEffectInstance().getEffect();
                for (net.minecraft.world.effect.MobEffect immunity : IMMUNITY_EFFECTS) {
                    if (effect == immunity) {
                        event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
                        break;
                    }
                }
            }
        }
    }

    /**
     * 玩家登录/重生时刷新属性
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.level().isClientSide()) {
            return;
        }
        UUID playerId = player.getUUID();

        // 刷新属性（处理死亡复活后丢失的修饰符）
        int[] data = playerSurgeryData.get(playerId);
        if (data != null && getTotalSurgeryLevel(player) > 0) {
            boolean needsRefresh = false;
            // 检查任意一种修饰符是否丢失
            if (data[SURGERY_ARMOR] > 0) {
                AttributeInstance armorAttr = player.getAttribute(Attributes.ARMOR);
                if (armorAttr != null && armorAttr.getModifier(ARMOR_UUID) == null) {
                    needsRefresh = true;
                }
            }
            if (data[SURGERY_DAMAGE] > 0) {
                AttributeInstance damageAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
                if (damageAttr != null && damageAttr.getModifier(DAMAGE_UUID) == null) {
                    needsRefresh = true;
                }
            }
            if (data[SURGERY_SPEED] > 0) {
                AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speedAttr != null && speedAttr.getModifier(SPEED_UUID) == null) {
                    needsRefresh = true;
                }
            }
            if (data[SURGERY_HEALTH] > 0) {
                AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
                if (maxHealthAttr != null && maxHealthAttr.getModifier(HEALTH_UUID) == null) {
                    needsRefresh = true;
                }
            }
            if (needsRefresh) {
                refreshAttributes(player);
            }
        }

        // 免疫效果：如果玩家拥有免疫效果则清除，绝不添加
        if (playerFirstImmunity.getOrDefault(playerId, false)) {
            for (net.minecraft.world.effect.MobEffect effect : IMMUNITY_EFFECTS) {
                if (player.hasEffect(effect)) {
                    player.removeEffect(effect);
                }
            }
        }
    }

    /**
     * 玩家死亡时清除手术效果和等级
     */
    @SubscribeEvent
    public static void onPlayerClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            Player original = event.getOriginal();
            Player newPlayer = event.getEntity();

            UUID originalId = original.getUUID();

            // 移除玩家的手术等级数据（所有类型全部归零）
            playerSurgeryData.remove(originalId);

            // 清除首次手术免疫状态（死亡后需要重新手术才能获得免疫）
            playerFirstImmunity.remove(originalId);

            // 清除NBT中的免疫标记
            newPlayer.getPersistentData().remove("SurgeryFirstImmunity");

            // 清除NBT中的手术等级数据（用于重新加载时恢复）
            CompoundTag nbt = newPlayer.getPersistentData();
            nbt.remove("SurgeryArmorLevel");
            nbt.remove("SurgeryDamageLevel");
            nbt.remove("SurgerySpeedLevel");
            nbt.remove("SurgeryHealthLevel");
            nbt.remove("SurgeryLuckLevel");

            // 刷新属性（清理残留的护甲/伤害/速度/跳跃/生命/幸运修饰符）
            refreshAttributes(newPlayer);
        }
    }

    /**
     * 玩家登录时恢复第一个手术的免疫状态
     * 通过检查NBT数据恢复免疫状态
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        UUID playerId = player.getUUID();
        CompoundTag nbt = player.getPersistentData();

        // 从NBT恢复手术等级数据
        int[] data = loadSurgeryLevelsFromNbt(player);
        // 只有当至少有一个手术等级大于0时才恢复
        if (data[SURGERY_ARMOR] > 0 || data[SURGERY_DAMAGE] > 0 || data[SURGERY_SPEED] > 0
                || data[SURGERY_HEALTH] > 0 || data[SURGERY_LUCK] > 0) {
            playerSurgeryData.put(playerId, data);
            refreshAttributes(player);
        }

        // 从NBT恢复免疫状态
        boolean hasImmunity = nbt.getBoolean("SurgeryFirstImmunity");
        if (hasImmunity) {
            playerFirstImmunity.put(playerId, true);
            // 清除可能残留的负面效果（万一之前被懊恼脚本或其他插件给加上）
            for (net.minecraft.world.effect.MobEffect effect : IMMUNITY_EFFECTS) {
                player.removeEffect(effect);
            }
        }
    }

    /**
     * 检查玩家是否拥有首次手术免疫
     */
    public static boolean hasFirstImmunity(Player player) {
        return playerFirstImmunity.getOrDefault(player.getUUID(), false);
    }

}