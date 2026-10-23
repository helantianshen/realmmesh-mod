// 仓库隔离校验：不启动世界，不读取日常游戏配置或玩家存档

import io.github.helantianshen.realmmesh.neoforge.warehouse.WarehouseInventory;
import io.github.helantianshen.realmmesh.neoforge.warehouse.WarehouseItemCodec;
import net.minecraft.SharedConstants;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayInputStream;
import java.util.Arrays;

/** 真实 ItemStack Codec 与背包算法的可重复 JVM 诊断 */
public final class WarehouseItemsDiagnostic {
    private static int checks;

    /** 初始化原版注册表后验证常见载荷、拒绝路径和整笔容量 */
    public static void main(String[] args) throws Exception {
        net.neoforged.fml.loading.LoadingModList.of(java.util.List.of(), java.util.List.of(),
                java.util.List.of(), java.util.List.of(), java.util.Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var registries = VanillaRegistries.createLookup();
        ItemStack stone = new ItemStack(Items.STONE, 64);
        var encoded = WarehouseItemCodec.encode(registries, stone);
        var restored = WarehouseItemCodec.decode(registries, encoded.itemId(), encoded.data());
        check(restored.getCount() == 1 && stone.getCount() == 64, "数量独立且原堆不变");
        var one = WarehouseItemCodec.encode(registries, stone.copyWithCount(1));
        check(Arrays.equals(encoded.data(), one.data()), "数量变化不改变单件载荷");

        ItemStack named = new ItemStack(Items.STONE);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("测试\u0000物品🪨"));
        var namedData = WarehouseItemCodec.encode(registries, named);
        check(!Arrays.equals(encoded.data(), namedData.data()), "改名物品保留独立数据");
        check(ItemStack.isSameItemSameComponents(named,
                WarehouseItemCodec.decode(registries, namedData.itemId(), namedData.data())), "中文及补充字符往返");
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamageValue(7);
        var swordData = WarehouseItemCodec.encode(registries, sword);
        check(!swordData.stackable(), "不可堆叠及耐久保留");
        check(WarehouseItemCodec.decode(registries, swordData.itemId(), swordData.data()).getDamageValue() == 7, "耐久往返");

        ItemStack enchanted = new ItemStack(Items.DIAMOND_SWORD);
        var sharpness = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
        enchanted.enchant(sharpness, 3);
        var enchantedData = WarehouseItemCodec.encode(registries, enchanted);
        ItemStack restoredEnchanted = WarehouseItemCodec.decode(registries, enchantedData.itemId(), enchantedData.data());
        check(ItemStack.isSameItemSameComponents(enchanted, restoredEnchanted), "附魔组件完整往返");
        check(restoredEnchanted.get(DataComponents.ENCHANTMENTS).getLevel(sharpness) == 3, "附魔种类与等级保留");

        var nested = new CompoundTag();
        var child = new CompoundTag();
        child.putInt("count", 17);
        child.putLongArray("data", new long[]{1, 2, Long.MAX_VALUE});
        nested.put("child", child);
        ItemStack custom = new ItemStack(Items.STONE);
        custom.set(DataComponents.CUSTOM_DATA, CustomData.of(nested));
        var customData = WarehouseItemCodec.encode(registries, custom);
        check(ItemStack.isSameItemSameComponents(custom,
                WarehouseItemCodec.decode(registries, customData.itemId(), customData.data())), "嵌套组件完整往返");

        reject(() -> WarehouseItemCodec.decode(registries, "missing:stone", encoded.data()), "注册 ID 不一致拒绝");
        reject(() -> WarehouseItemCodec.decode(registries, encoded.itemId(), Arrays.copyOf(encoded.data(), encoded.data().length + 1)), "尾部字节拒绝");
        var tag = NbtIo.read(new DataInputStream(new ByteArrayInputStream(encoded.data())));
        tag.putInt("unexpected", 2);
        var bytes = new ByteArrayOutputStream();
        NbtIo.writeUnnamedTag(tag, new DataOutputStream(bytes));
        reject(() -> WarehouseItemCodec.decode(registries, encoded.itemId(), bytes.toByteArray()), "有损未知根字段拒绝");
        reject(() -> WarehouseItemCodec.decode(registries, encoded.itemId(), new byte[WarehouseItemCodec.MAX_PAYLOAD_BYTES + 1]), "大载荷拒绝");
        ItemStack oversized = new ItemStack(Items.STONE);
        oversized.set(DataComponents.CUSTOM_NAME, Component.literal("x".repeat(70000)));
        reject(() -> WarehouseItemCodec.encode(registries, oversized), "超长 UTF 拒绝且不写空字符串");

        var unknownItem = new CompoundTag();
        unknownItem.putString("id", "missing:unknown_item");
        reject(() -> WarehouseItemCodec.decode(registries, "missing:unknown_item", binary(unknownItem)),
                "实际未知物品注册 ID 的载荷拒绝");
        var unknownComponents = new CompoundTag();
        unknownComponents.putString("id", "minecraft:stone");
        var components = new CompoundTag();
        components.putInt("minecraft:damage", 1);
        components.putString("missing:unknown_component", "unknown");
        unknownComponents.put("components", components);
        reject(() -> WarehouseItemCodec.decode(registries, "minecraft:stone", binary(unknownComponents)),
                "合法与未知组件混合的 partial 结果拒绝");
        var deeplyNested = new CompoundTag();
        var current = deeplyNested;
        for (int i = 0; i < 70; i++) {
            var next = new CompoundTag();
            current.put("next", next);
            current = next;
        }
        ItemStack deep = new ItemStack(Items.STONE);
        deep.set(DataComponents.CUSTOM_DATA, CustomData.of(deeplyNested));
        reject(() -> WarehouseItemCodec.encode(registries, deep), "写入前拒绝过深原生 NBT 树");
        var manyNodes = new CompoundTag();
        var list = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < 65536; i++) list.add(net.minecraft.nbt.ByteTag.valueOf((byte) 1));
        manyNodes.put("list", list);
        ItemStack wide = new ItemStack(Items.STONE);
        wide.set(DataComponents.CUSTOM_DATA, CustomData.of(manyNodes));
        reject(() -> WarehouseItemCodec.encode(registries, wide), "写入前拒绝过多原生 NBT 节点");

        var inventory = new Inventory(null);
        inventory.items.set(0, new ItemStack(Items.STONE, 60));
        var plan = WarehouseInventory.plan(inventory, restored, 70);
        check(plan != null && plan.get(0).getCount() == 64 && plan.get(1).getCount() == 64
                && plan.get(2).getCount() == 2, "按实际上限拆分和补堆");
        check(inventory.items.get(0).getCount() == 60 && inventory.items.get(1).isEmpty(), "预检查不改背包");
        for (int i = 0; i < 36; i++) inventory.items.set(i, new ItemStack(Items.DIRT, 64));
        check(WarehouseInventory.plan(inventory, restored, 1) == null, "满背包整笔拒绝");
        inventory.items.set(0, ItemStack.EMPTY);
        check(WarehouseInventory.plan(inventory, restored, 65) == null, "差一件也不部分发放");
        check(WarehouseInventory.plan(inventory, sword, 2) == null, "不可堆叠每件占一个槽位");
        inventory.items.set(1, ItemStack.EMPTY);
        var swords = WarehouseInventory.plan(inventory, sword, 2);
        check(swords != null && swords.get(0).getCount() == 1 && swords.get(1).getCount() == 1,
                "不可堆叠分入两个空槽");
        var pearls = WarehouseInventory.plan(inventory, new ItemStack(Items.ENDER_PEARL), 17);
        check(pearls != null && pearls.get(0).getCount() == 16 && pearls.get(1).getCount() == 1,
                "16 件堆叠上限不写死为 64");
        check(WarehouseInventory.plan(inventory, restored, 0) == null, "零数量拒绝");
        inventory.items.set(0, new ItemStack(Items.STONE, 63));
        inventory.items.set(1, new ItemStack(Items.DIRT, 64));
        check(WarehouseInventory.plan(inventory, named, 1) == null, "改名物品不能并入普通堆");
        check(inventory.items.get(0).getCount() == 63, "失败规划不改变原堆");
        if (args.length == 1) {
            var directory = java.nio.file.Path.of(args[0]);
            java.nio.file.Files.createDirectories(directory);
            java.nio.file.Files.write(directory.resolve("stone.nbt"), encoded.data());
            java.nio.file.Files.write(directory.resolve("named.nbt"), namedData.data());
            java.nio.file.Files.write(directory.resolve("damaged.nbt"), swordData.data());
            java.nio.file.Files.write(directory.resolve("nested.nbt"), customData.data());
            java.nio.file.Files.write(directory.resolve("enchanted.nbt"), enchantedData.data());
        }
        System.out.println("warehouse_item_checks_passed=" + checks);
    }

    /** 使用真实原生编码构造非法语义载荷，不绕过被测解码器 */
    private static byte[] binary(CompoundTag tag) {
        try {
            var bytes = new ByteArrayOutputStream();
            NbtIo.writeUnnamedTag(tag, new DataOutputStream(bytes));
            return bytes.toByteArray();
        } catch (java.io.IOException error) {
            throw new AssertionError(error);
        }
    }

    /** 每个断言失败立即退出，成功累计检查数 */
    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
        checks++;
    }

    /** 必须产生明确拒绝，不能用成功但丢失数据的结果通过检查 */
    private static void reject(Runnable action, String name) {
        try {
            action.run();
        } catch (RuntimeException expected) {
            checks++;
            return;
        }
        throw new AssertionError(name);
    }
}
