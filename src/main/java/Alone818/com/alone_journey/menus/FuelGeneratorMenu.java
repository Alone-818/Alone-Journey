package Alone818.com.alone_journey.menus;

import Alone818.com.alone_journey.blockentities.FuelGeneratorBlockEntity;
import Alone818.com.alone_journey.init.ModBlocks;
import Alone818.com.alone_journey.init.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 燃料发电机菜单
 *
 * 槽位布局：
 * - 1个燃料槽（左侧）
 * - 4个升级槽（中间，为机器升级系统预留）
 * - 玩家背包 + 快捷栏
 */
public class FuelGeneratorMenu extends AbstractContainerMenu {

    // 机器槽位索引
    public static final int FUEL_SLOT = 0;
    public static final int FIRST_UPGRADE_SLOT = 1;
    public static final int LAST_UPGRADE_SLOT = 4;

    // 玩家槽位索引范围
    private static final int INV_SLOT_START = 5;
    private static final int INV_SLOT_END = 32;
    private static final int HOTBAR_SLOT_START = 32;
    private static final int HOTBAR_SLOT_END = 41;

    // 槽位界面坐标（与贴图槽位对齐，物品渲染位置 = 贴图槽位左上角 + 1）
    private static final int FUEL_SLOT_X = 25;
    private static final int FUEL_SLOT_Y = 29;
    private static final int UPGRADE_SLOT_X = 47;
    private static final int UPGRADE_SLOT_Y = 57;

    private final FuelGeneratorBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    // 客户端通过网络数据反序列化方块位置
    public FuelGeneratorMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, pos));
    }

    public FuelGeneratorMenu(int containerId, Inventory playerInventory, @Nullable FuelGeneratorBlockEntity blockEntity) {
        super(ModMenus.FUEL_GENERATOR.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), blockEntity.getBlockPos());
        this.data = blockEntity.getDataAccess();

        // 燃料槽（左侧）
        this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), FUEL_SLOT, FUEL_SLOT_X, FUEL_SLOT_Y));

        // 4个升级槽（中间）
        for (int i = 0; i < 4; i++) {
            this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(),
                    FIRST_UPGRADE_SLOT + i, UPGRADE_SLOT_X + i * 18, UPGRADE_SLOT_Y));
        }

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

    private static FuelGeneratorBlockEntity getBlockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof FuelGeneratorBlockEntity generator) {
            return generator;
        }
        throw new IllegalStateException("方块实体类型错误: " + pos);
    }

    // 客户端同步数据访问
    public int getEnergyStored() {
        return this.data.get(0);
    }

    public int getMaxEnergyStored() {
        return this.data.get(1);
    }

    public int getBurnRemaining() {
        return this.data.get(2);
    }

    public int getBurnMax() {
        return this.data.get(3);
    }

    public int getBurnRate() {
        return this.data.get(4);
    }

    public int getCurrentGeneration() {
        return this.data.get(5);
    }

    public boolean isBurning() {
        return this.data.get(6) > 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index <= LAST_UPGRADE_SLOT) {
            // 机器槽 -> 玩家背包/快捷栏
            if (!this.moveItemStackTo(stack, INV_SLOT_START, HOTBAR_SLOT_END, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // 玩家 -> 燃料槽（仅可燃物）
            if (ForgeHooks.getBurnTime(stack, null) > 0) {
                if (!this.moveItemStackTo(stack, FUEL_SLOT, FUEL_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // 其他物品在背包与快捷栏之间移动
            if (index < INV_SLOT_END) {
                if (!this.moveItemStackTo(stack, HOTBAR_SLOT_START, HOTBAR_SLOT_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, INV_SLOT_START, INV_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }
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
        return stillValid(this.access, player, ModBlocks.FUEL_GENERATOR.get());
    }
}
