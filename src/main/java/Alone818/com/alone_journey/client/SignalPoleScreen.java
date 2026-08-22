package Alone818.com.alone_journey.client;

import Alone818.com.alone_journey.menus.SignalPoleMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 信号杆界面（电网节点通用界面的具体实现，供菜单注册使用）
 */
public class SignalPoleScreen extends NetworkNodeScreen<SignalPoleMenu> {

    public SignalPoleScreen(SignalPoleMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
