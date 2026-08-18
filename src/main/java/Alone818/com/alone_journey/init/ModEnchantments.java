package Alone818.com.alone_journey.init;

import Alone818.com.alone_journey.Alone_journey;
import Alone818.com.alone_journey.enchant.high_speed_slash;
import Alone818.com.alone_journey.enchant.meticulous_craft;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, Alone_journey.MODID);

    public static final RegistryObject<Enchantment> METICULOUS_CRAFT =
            ENCHANTMENTS.register("meticulous_craft", meticulous_craft::new);

    public static final RegistryObject<Enchantment> HIGH_SPEED_SLASH =
            ENCHANTMENTS.register("high_speed_slash", high_speed_slash::new);

    public static void register(IEventBus eventBus) {
        ENCHANTMENTS.register(eventBus);
    }
}