package Alone818.com.alone_journey.init;

import Alone818.com.alone_journey.Alone_journey;
import Alone818.com.alone_journey.events.ShieldEvent;
import net.minecraftforge.common.MinecraftForge;

public class ModEvents {
    public static void register() {
        MinecraftForge.EVENT_BUS.register(ShieldEvent.class);
    }
}
