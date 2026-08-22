package Alone818.com.alone_journey.menus;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * 虚空采矿机菜单
 *
 * 功能特性：
 * - 使用原版箱子界面（GENERIC_9X3）
 * - 27格机械仓库库存 + 玩家背包
 */
public class VoidMinerMenu extends ChestMenu {

    public VoidMinerMenu(int containerId, Inventory playerInventory, Container container) {
        super(MenuType.GENERIC_9x3, containerId, playerInventory, container, 3);
    }
}
