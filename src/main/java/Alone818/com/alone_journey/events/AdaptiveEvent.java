package Alone818.com.alone_journey.events;

import Alone818.com.alone_journey.init.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.util.ICuriosHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Alone818.com.alone_journey.Alone_journey.MODID)
public class AdaptiveEvent {

    // 单种伤害类型的适应层数上限
    private static final int MAX_LEVEL = 5;
    // 每层适应提供的减免比例（5 层 = 75%）
    private static final double REDUCTION_PER_LEVEL = 0.15;

    // 服务端记录每个玩家对每种伤害类型的适应层数（伤害类型 -> 层数）
    private static final Map<UUID, Map<String, Integer>> ADAPTATION_LEVELS = new HashMap<>();

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player) || event.getAmount() <= 0) {
            return;
        }
        ICuriosHelper helper = CuriosApi.getCuriosHelper();
        if (helper.findFirstCurio(player, ModItems.ADAPTIVE_FLESH.get()).isEmpty()) {
            return;
        }

        // 伤害类型标识（如 mob / arrow / fall / magic，与伤害来源无关）
        String type = event.getSource().type().msgId();
        Map<String, Integer> levels = ADAPTATION_LEVELS.computeIfAbsent(player.getUUID(), k -> new HashMap<>());

        // 1. 先按该类型当前层数结算减免（层数影响的是“下一次”受伤）
        int level = levels.getOrDefault(type, 0);
        if (level > 0) {
            double reduction = Math.min(1.0, level * REDUCTION_PER_LEVEL);
            event.setAmount(event.getAmount() * (float) (1.0 - reduction));
        }

        // 2. 再记录本次：其他类型的适应层数各衰减 1 层（归零则移除）
        levels.replaceAll((t, v) -> t.equals(type) ? v : v - 1);
        levels.values().removeIf(v -> v <= 0);

        // 3. 本次类型层数 +1（上限 MAX_LEVEL）
        levels.merge(type, 1, Integer::sum);
        if (levels.get(type) > MAX_LEVEL) {
            levels.put(type, MAX_LEVEL);
        }
    }

    // 玩家退出时清理记录，防止长期驻留内存
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ADAPTATION_LEVELS.remove(event.getEntity().getUUID());
    }
}
