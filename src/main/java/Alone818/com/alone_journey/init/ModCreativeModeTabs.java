package Alone818.com.alone_journey.init;

import Alone818.com.alone_journey.Alone_journey;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Alone_journey.MODID);

    // 注册本模组的创造模式标签。
    public static final RegistryObject<CreativeModeTab> ALONEITEMS =
            CREATIVE_MODE_TABS.register("alone_journey.curios",
                    () -> CreativeModeTab.builder()

                            // 设置创造标签在界面中显示的图标。
                            // 这里使用石头作为示例图标，后续可以替换为模组物品。
                            .icon(() -> new ItemStack(Items.STONE))

                            // 设置标签的显示名称。
                            // 使用可本地化文本（语言文件中定义）。
                            .title(Component.translatable("tab.alone_journey.curios"))

                            // 定义该标签中显示的物品内容。
                            // output.accept(...) 用于向标签中添加物品。
                            .displayItems((itemDisplayParameters,
                                           output) -> {
                                output.accept(ModItems.CRYSTALLINE_HEART.get());
                                output.accept(ModItems.BLEEDINGSHIELD.get());
                            })

                            // 构建最终的 CreativeModeTab 实例。
                            .build());

    public static void register(IEventBus eventBus){
        CREATIVE_MODE_TABS.register(eventBus);
    }
}