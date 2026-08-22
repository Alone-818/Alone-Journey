package Alone818.com.alone_journey.menus;

import Alone818.com.alone_journey.blockentities.PowerPylonBlockEntity;
import Alone818.com.alone_journey.init.ModBlocks;
import Alone818.com.alone_journey.init.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 用电桩菜单（与信号杆界面相同）
 *
 * 布局（176×166）：无机器槽位，玩家背包 + 快捷栏
 *
 * 同步数据（索引）：0=是否接入电网 1=电网总电量 2=电网储能上限
 */
public class PowerPylonMenu extends AbstractContainerMenu implements NetworkStatusMenu {

    // 玩家槽位索引范围
    private static final int INV_SLOT_START = 0;
    private static final int INV_SLOT_END = 27;
    private static final int HOTBAR_SLOT_START = 27;
    private static final int HOTBAR_SLOT_END = 36;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final BlockPos pos;

    // 客户端通过网络数据反序列化方块位置
    public PowerPylonMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, pos));
    }

    public PowerPylonMenu(int containerId, Inventory playerInventory, @Nullable PowerPylonBlockEntity blockEntity) {
        super(ModMenus.POWER_PYLON.get(), containerId);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), blockEntity.getBlockPos());
        this.pos = blockEntity.getBlockPos();
        this.data = blockEntity.getMenuData();

        // 玩家背包
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // 快捷栏
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        this.addDataSlots(this.data);
    }

    private static PowerPylonBlockEntity getBlockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof PowerPylonBlockEntity pylon) {
            return pylon;
        }
        throw new IllegalStateException("方块实体类型错误: " + pos);
    }

    /**
     * 用电桩方块位置（用于连接电线按键数据包）
     */
    @Override
    public BlockPos getPos() {
        return this.pos;
    }

    // 客户端同步数据访问
    @Override
    public boolean hasNetwork() {
        return this.data.get(0) > 0;
    }

    @Override
    public int getNetworkEnergy() {
        return this.data.get(1);
    }

    @Override
    public int getNetworkCapacity() {
        return this.data.get(2);
    }

    @Override
    public int getRateIn(int mode) {
        return this.data.get(3 + mode * 2);
    }

    @Override
    public int getRateOut(int mode) {
        return this.data.get(4 + mode * 2);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        // 仅玩家槽位之间移动（背包 <-> 快捷栏）
        if (index < INV_SLOT_END) {
            if (!this.moveItemStackTo(stack, HOTBAR_SLOT_START, HOTBAR_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, INV_SLOT_START, INV_SLOT_END, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.POWER_PYLON.get());
    }
}
