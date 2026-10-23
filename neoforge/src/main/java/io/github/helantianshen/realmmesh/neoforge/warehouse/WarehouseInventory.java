// 仓库背包操作：只处理主背包 36 格，先计算完整方案再修改真实槽位

package io.github.helantianshen.realmmesh.neoforge.warehouse;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.List;

/** 全部方法只能在游戏线程使用，网络等待后必须重新计算容量 */
public final class WarehouseInventory {
    private static final int MAIN_SLOTS = 36;

    /** 工具类不保存任何背包快照 */
    private WarehouseInventory() {}

    /** 只允许生存或冒险、存活且没有外部容器与鼠标持物的玩家操作 */
    public static boolean available(ServerPlayer player) {
        GameType mode = player.gameMode.getGameModeForPlayer();
        return player.isAlive() && !player.isRemoved()
                && (mode == GameType.SURVIVAL || mode == GameType.ADVENTURE)
                && player.containerMenu == player.inventoryMenu
                && player.containerMenu.getCarried().isEmpty();
    }

    /** 返回足够容纳整笔数量的槽位副本，容量不足时不改变真实背包 */
    public static List<ItemStack> plan(Inventory inventory, ItemStack single, int amount) {
        if (single.isEmpty() || amount <= 0 || inventory.items.size() != MAIN_SLOTS) return null;
        var slots = new ArrayList<ItemStack>(MAIN_SLOTS);
        for (ItemStack stack : inventory.items) slots.add(stack.copy());
        int remaining = amount;
        // 先填相同组件的现有堆，再使用空槽，不依赖可能部分成功的 Inventory.add
        for (int index = 0; index < slots.size() && remaining > 0; index++) {
            ItemStack stack = slots.get(index);
            if (stack.isEmpty() || !stack.isStackable() || !ItemStack.isSameItemSameComponents(stack, single)) continue;
            int space = Math.max(0, inventory.getMaxStackSize(stack) - stack.getCount());
            int moved = Math.min(remaining, space);
            stack.grow(moved);
            remaining -= moved;
        }
        for (int index = 0; index < slots.size() && remaining > 0; index++) {
            if (!slots.get(index).isEmpty()) continue;
            int limit = single.isStackable() ? inventory.getMaxStackSize(single) : 1;
            if (limit <= 0) return null;
            int moved = Math.min(remaining, limit);
            slots.set(index, single.copyWithCount(moved));
            remaining -= moved;
        }
        return remaining == 0 ? slots : null;
    }

    /** 在同一次游戏线程调用内重新规划并整笔发放，不掉落或部分给予 */
    public static boolean give(ServerPlayer player, ItemStack single, int amount) {
        if (!available(player)) return false;
        var inventory = player.getInventory();
        var slots = plan(inventory, single, amount);
        if (slots == null) return false;
        for (int index = 0; index < slots.size(); index++) {
            if (!ItemStack.matches(inventory.items.get(index), slots.get(index))) {
                inventory.items.set(index, slots.get(index));
            }
        }
        inventory.setChanged();
        player.inventoryMenu.broadcastChanges();
        return true;
    }
}
