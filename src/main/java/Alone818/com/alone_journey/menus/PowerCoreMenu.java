package Alone818.com.alone_journey.menus;

import Alone818.com.alone_journey.blockentities.PowerCoreBlockEntity;
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
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 控制核心菜单（信标风格）
 *
 * 布局（230×219）：
 * - 核心升级槽 (54,110)：放入核心升级组件立即消耗，核心等级 +1（最高5级）
 * - 3个专属插件槽 (118,110) 起：为核心升级插件系统预留，暂不接受物品
 * - 玩家背包 + 快捷栏（230宽界面居中：x = 34）
 *
 * 同步数据（索引）：0=当前电量 1=储能上限 2=信号杆数 3=网络机器数 4=核心等级
 */
public class PowerCoreMenu extends AbstractContainerMenu {

    // 机器槽位索引
    public static final int UPGRADE_SLOT = 0;
    public static final int PLUGIN_SLOT_START = 1;
    public static final int PLUGIN_SLOT_END = 3;

    // 玩家槽位索引范围
    private static final int INV_SLOT_START = 4;
    private static final int INV_SLOT_END = 31;
    private static final int HOTBAR_SLOT_START = 31;
    private static final int HOTBAR_SLOT_END = 40;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final BlockPos pos;
    private final PowerCoreBlockEntity blockEntity;

    // 客户端通过网络数据反序列化方块位置
    public PowerCoreMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, pos));
    }

    public PowerCoreMenu(int containerId, Inventory playerInventory, @Nullable PowerCoreBlockEntity blockEntity) {
        super(ModMenus.POWER_CORE.get(), containerId);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), blockEntity.getBlockPos());
        this.pos = blockEntity.getBlockPos();
        this.blockEntity = blockEntity;
        this.data = blockEntity.getMenuData();

        // 核心升级槽
        this.addSlot(new SlotItemHandler(blockEntity.getUpgradeInventory(), 0, 54, 110));

        // 3个专属插件槽（间隔18像素）
        for (int i = 0; i < 3; i++) {
            this.addSlot(new SlotItemHandler(blockEntity.getUpgradeInventory(),
                    PLUGIN_SLOT_START + i, 118 + i * 18, 110));
        }

        // 玩家背包
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 34 + col * 18, 137 + row * 18));
            }
        }

        // 快捷栏
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 34 + col * 18, 195));
        }

        this.addDataSlots(this.data);
    }

    private static PowerCoreBlockEntity getBlockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof PowerCoreBlockEntity core) {
            return core;
        }
        throw new IllegalStateException("方块实体类型错误: " + pos);
    }

    /**
     * 核心方块位置（用于电网连接按键数据包）
     */
    public BlockPos getPos() {
        return this.pos;
    }

    // 客户端同步数据访问
    public int getEnergyStored() {
        return this.data.get(0);
    }

    public int getCapacity() {
        return this.data.get(1);
    }

    public int getPoleCount() {
        return this.data.get(2);
    }

    public int getMachineCount() {
        return this.data.get(3);
    }

    public int getCoreLevel() {
        return this.data.get(4);
    }

    /**
     * 吞吐速率读取（FE/s）
     *
     * @param mode 0=即时 1=5分钟 2=10分钟
     */
    public int getRateIn(int mode) {
        return this.data.get(5 + mode * 2);
    }

    public int getRateOut(int mode) {
        return this.data.get(6 + mode * 2);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index <= PLUGIN_SLOT_END) {
            // 机器槽 -> 玩家背包/快捷栏
            if (!this.moveItemStackTo(stack, INV_SLOT_START, HOTBAR_SLOT_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (this.blockEntity.isUpgradeItem(stack)) {
            // 玩家 -> 升级槽（仅升级物品）
            if (!this.moveItemStackTo(stack, UPGRADE_SLOT, UPGRADE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < INV_SLOT_END) {
            // 背包 <-> 快捷栏
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
        return stillValid(this.access, player, ModBlocks.POWER_CORE.get());
    }
}
