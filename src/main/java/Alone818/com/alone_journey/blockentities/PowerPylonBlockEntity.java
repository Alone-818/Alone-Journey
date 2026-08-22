package Alone818.com.alone_journey.blockentities;

import Alone818.com.alone_journey.blocks.AbstractPoleBlock;
import Alone818.com.alone_journey.init.ModBlockEntities;
import Alone818.com.alone_journey.menus.PowerPylonMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 用电桩方块实体
 *
 * 功能特性：
 * - 周围 11×11×11 区域为供电区域（半径5，大于信号杆的5×5×5）
 * - 电网节点之间的连接渲染为带重力下垂的黑弧线（连接数据同步到客户端）
 * - 右键打开电网界面：显示电网总电量、连接电线按键
 */
public class PowerPylonBlockEntity extends NetworkNodeBlockEntity {

    // 电线渲染挂点：最高处方块的中心点（整根桩 1×1×5）
    public static final double WIRE_TOP_OFFSET = AbstractPoleBlock.HEIGHT;

    public PowerPylonBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POWER_PYLON.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.alone_journey.power_pylon");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PowerPylonMenu(containerId, playerInventory, worldPosition);
    }

    // PowerLinkable 接口实现

    @Override
    public int getPowerRadius() {
        return 5; // 11×11×11 供电区域
    }

    @Override
    public double getWireAnchorHeight() {
        return WIRE_TOP_OFFSET;
    }
}
