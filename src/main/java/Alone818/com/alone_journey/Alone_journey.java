package Alone818.com.alone_journey;

import Alone818.com.alone_journey.init.ModEffects;
import Alone818.com.alone_journey.init.ModBlocks;
import Alone818.com.alone_journey.init.ModCreativeModeTabs;
import Alone818.com.alone_journey.init.ModEvents;
import Alone818.com.alone_journey.init.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Alone_journey.MODID)
public class Alone_journey{
    public static final String MODID = "alone_journey";
    private static final Logger LOGGER = LogUtils.getLogger();

        public Alone_journey() {
            IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
            //start region
            ModItems.register(modEventBus);
            ModBlocks.register(modEventBus);
            ModCreativeModeTabs.register(modEventBus);
            ModEffects.register(modEventBus);
            //end region

            ModEvents.register();

            modEventBus.addListener(this::commonSetup);
            MinecraftForge.EVENT_BUS.register(this);

            // 注册配置文件，使其在 config/ 目录下生成 alone_journey-server.toml
            ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        }

        private void commonSetup(final FMLCommonSetupEvent event) {}

}