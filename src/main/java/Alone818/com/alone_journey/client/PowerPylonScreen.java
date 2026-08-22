package Alone818.com.alone_journey.client;

import Alone818.com.alone_journey.menus.PowerPylonMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 用电桩界面（电网节点通用界面的具体实现，供菜单注册使用）
 */
public class PowerPylonScreen extends NetworkNodeScreen<PowerPylonMenu> {

    public PowerPylonScreen(PowerPylonMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
