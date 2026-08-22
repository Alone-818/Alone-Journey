package Alone818.com.alone_journey.blockentities;

import Alone818.com.alone_journey.blocks.AbstractPoleBlock;
import Alone818.com.alone_journey.init.ModBlockEntities;
import Alone818.com.alone_journey.menus.SignalPoleMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 信号杆方块实体
 *
 * 功能特性：
 * - 周围 5×5×5 区域为电力区域，区域内设备接入电网
 * - 右键打开电网界面：显示电网总电量、连接电线按键
 */
public class SignalPoleBlockEntity extends NetworkNodeBlockEntity {

    public SignalPoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIGNAL_POLE.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.alone_journey.signal_pole");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SignalPoleMenu(containerId, playerInventory, worldPosition);
    }

    @Override
    public double getWireAnchorHeight() {
        // 导线挂点：最高处方块的中心点（整根杆 1×1×5）
        return AbstractPoleBlock.HEIGHT;
    }
}
