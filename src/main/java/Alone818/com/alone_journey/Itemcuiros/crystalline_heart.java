package Alone818.com.alone_journey.Itemcuiros;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.UUID;

public class crystalline_heart extends Item implements ICurioItem {

    // 用于记录生命 reductions 的 NBT key
    public static final String NB_TAG_REDUCTION = "healthReduction";
    public static final String NB_TAG_SHIELD = "ShieldReduction";
    // 修饰符的固定 UUID（确保同一饰品多个实例不会叠加）
    private static final UUID HEALTH_MODIFIER_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

    public crystalline_heart() {
        super(new Properties()
                .stacksTo(1)
        );
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();

        // 减少80%最大生命值
        // 使用 MULTIPLY_TOTAL 会将玩家所有的额外生命值加成也一同缩小 80%
        // 操作：(1 + modifiers_sum) * (1 + 额外加成) * (1 - 0.8)
        modifiers.put(Attributes.MAX_HEALTH, new AttributeModifier(
                uuid,
                "Crystalline Heart Max Health Reduction",
                -0.8,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        ));

        return modifiers;
    }

    /**
     * 穿上饰品时，通过 NBT 记录被减少了多少生命值。
     * 由于属性修饰符是动态计算的，通过当前玩家总最大生命值 / (1 - 0.8) 得到基础值，从而计算出减少量。
     */
    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        if (slotContext.entity() instanceof net.minecraft.world.entity.player.Player player) {
            CompoundTag tag = stack.getOrCreateTag();
            // 在此记录属性修饰符生效后的实际数值
            double currentMaxHealth = player.getMaxHealth();
            // 被减少的值 = 原始值 * 0.8 = (当前值 / 0.2) * 0.8 = 当前值 * 4
            tag.putDouble(NB_TAG_REDUCTION, currentMaxHealth * 4.0);

            int perhealthtoshield =4;
            tag.putDouble(NB_TAG_SHIELD, currentMaxHealth * 4.0/perhealthtoshield);
        }
    }

    /**
     * 脱下饰品时调用：清空 NBT 中的记录
     */
    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(NB_TAG_REDUCTION);
            tag.remove(NB_TAG_SHIELD);
            if (tag.isEmpty()) {
                stack.setTag(null);
            }
        }
    }
}
