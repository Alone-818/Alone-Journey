package Alone818.com.alone_journey;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = Alone_journey.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER.comment("Whether to log the dirt block on common setup").define("logDirtBlock", true);

    private static final ForgeConfigSpec.IntValue MAGIC_NUMBER = BUILDER.comment("A magic number").defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER.comment("What you want the introduction message to be for the magic number").define("magicNumberIntroduction", "The magic number is... ");

    // a list of strings that are treated as resource locations for items
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER.comment("A list of items to log on common setup.").defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), Config::validateItemName);

    // 护盾系统配置
    private static final ForgeConfigSpec.DoubleValue ARMOR_PER_SHIELD_MAX =
            BUILDER.comment("每多少点护甲增加 1 点护盾上限",
                    "默认值: 5.0  (即每 5 点护甲 +1 护盾上限)")
                   .defineInRange("armorPerShieldMax", 5.0, 0.1, 100.0);

    private static final ForgeConfigSpec.DoubleValue TOUGHNESS_PER_DURATION_SEC =
            BUILDER.comment("每多少点韧性增加 1 秒抗性 V 持续时间",
                    "默认值: 4.0  (即每 4 点韧性 +1s 抗性时长)")
                   .defineInRange("toughnessPerDurationSec", 4.0, 0.1, 100.0);

    private static final ForgeConfigSpec.DoubleValue ARMOR_HEAL_SPEED_PERCENT =
            BUILDER.comment("每 1 点护甲增加多少百分比的护盾回复速度",
                    "默认值: 0.05  (即每点护甲使冷却时间缩短 5%)")
                   .defineInRange("armorHealSpeedPercent", 0.05, 0.0, 1.0);

    // 武器技能配置
    private static final ForgeConfigSpec.IntValue CHAINSWOVERCLOCK_DURATION =
            BUILDER.comment("链锯剑超频（振砍）技能持续时长（tick），20 tick = 1 秒",
                    "默认值: 60  (3 秒)")
                   .defineInRange("chainsword.overclockDuration", 60, 1, 1200);

    private static final ForgeConfigSpec.IntValue CHAINSWOVERCLOCK_COOLDOWN =
            BUILDER.comment("链锯剑超频技能冷却时长（tick），20 tick = 1 秒，从技能结束后开始计时",
                    "默认值: 300  (15 秒)")
                   .defineInRange("chainsword.overclockCooldown", 300, 0, 1200);

    private static final ForgeConfigSpec.IntValue POWERSWORD_OVERCLOCK_DURATION =
            BUILDER.comment("动力剑超频技能持续时长（tick），20 tick = 1 秒",
                    "默认值: 260  (13 秒)")
                   .defineInRange("powersword.overclockDuration", 260, 1, 1200);

    private static final ForgeConfigSpec.IntValue POWERSWORD_OVERCLOCK_COOLDOWN =
            BUILDER.comment("动力剑超频技能冷却时长（tick），20 tick = 1 秒，从技能结束后开始计时",
                    "默认值: 300  (15 秒)")
                   .defineInRange("powersword.overclockCooldown", 300, 0, 1200);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean logDirtBlock;
    public static int magicNumber;
    public static String magicNumberIntroduction;
    public static Set<Item> items;

    // 护盾动态变量（由配置文件加载）
    public static double armorPerShieldMax;
    public static double toughnessPerDurationSec;
    public static double armorHealSpeedPercent;

    // 武器技能变量（由配置文件加载）
    public static int chainswordOverclockDuration;
    public static int chainswordOverclockCooldown;
    public static int powerswordOverclockDuration;
    public static int powerswordOverclockCooldown;

    private static boolean validateItemName(final Object obj) {
        return obj instanceof final String itemName && ForgeRegistries.ITEMS.containsKey(new ResourceLocation(itemName));
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        logDirtBlock = LOG_DIRT_BLOCK.get();
        magicNumber = MAGIC_NUMBER.get();
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();

        // convert the list of strings into a set of items
        items = ITEM_STRINGS.get().stream().map(itemName -> ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemName))).collect(Collectors.toSet());

        // 加载护盾系统配置
        armorPerShieldMax = ARMOR_PER_SHIELD_MAX.get();
        toughnessPerDurationSec = TOUGHNESS_PER_DURATION_SEC.get();
        armorHealSpeedPercent = ARMOR_HEAL_SPEED_PERCENT.get();

        // 加载武器技能配置
        chainswordOverclockDuration = CHAINSWOVERCLOCK_DURATION.get();
        chainswordOverclockCooldown = CHAINSWOVERCLOCK_COOLDOWN.get();
        powerswordOverclockDuration = POWERSWORD_OVERCLOCK_DURATION.get();
        powerswordOverclockCooldown = POWERSWORD_OVERCLOCK_COOLDOWN.get();
    }
}
