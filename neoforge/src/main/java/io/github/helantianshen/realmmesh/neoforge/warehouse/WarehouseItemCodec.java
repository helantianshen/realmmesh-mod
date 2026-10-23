// 仓库载荷：严格使用游戏注册表的持久化 Codec 编解码单件物品
// 数量独立传输，编码失败、未知组件及有损往返均拒绝进入资产流程

package io.github.helantianshen.realmmesh.neoforge.warehouse;

import com.mojang.serialization.DataResult;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/** 单件载荷采用未压缩、大端、空根名称的 Compound NBT */
public final class WarehouseItemCodec {
    public static final int MAX_PAYLOAD_BYTES = 1024 * 1024;
    private static final int MAX_NBT_DEPTH = 64;
    private static final int MAX_NBT_NODES = 65536;
    private static final int MAX_NBT_HEAP_BYTES = 16 * MAX_PAYLOAD_BYTES;

    /** 工具类不持有注册表或玩家对象 */
    private WarehouseItemCodec() {}

    /** 在扩容前限制编码字节数，避免先生成超大载荷再拒绝 */
    private static final class LimitedOutput extends ByteArrayOutputStream {
        /** 单字节写入也先检查容量 */
        @Override
        public synchronized void write(int value) {
            if (count == MAX_PAYLOAD_BYTES) throw new IllegalArgumentException("物品载荷超过限制");
            super.write(value);
        }

        /** 批量写入不得使缓冲超过单件上限 */
        @Override
        public synchronized void write(byte[] data, int offset, int length) {
            if (length > MAX_PAYLOAD_BYTES - count) throw new IllegalArgumentException("物品载荷超过限制");
            super.write(data, offset, length);
        }
    }

    /** 只保留资产传输所需信息，二进制数组由本次操作独占 */
    public record Payload(String itemId, byte[] data, boolean stackable) {}

    /** 编码单件物品并验证恢复结果，所有失败必须发生在扣除背包之前 */
    public static Payload encode(HolderLookup.Provider registries, ItemStack original) {
        if (original.isEmpty()) throw new IllegalArgumentException("空物品不能存入仓库");
        ItemStack single = original.copyWithCount(1);
        CompoundTag tag = encodeTag(registries, single);
        validateStructure(tag, 1, new int[1]);
        try {
            var bytes = new LimitedOutput();
            // 不使用 write 的字符串回退，超长 UTF 必须失败，不能被静默替换为空字符串
            NbtIo.writeUnnamedTag(tag, new DataOutputStream(bytes));
            byte[] data = bytes.toByteArray();
            String itemId = BuiltInRegistries.ITEM.getKey(single.getItem()).toString();
            ItemStack restored = decode(registries, itemId, data);
            if (!ItemStack.isSameItemSameComponents(single, restored)) {
                throw new IllegalArgumentException("物品包含无法完整持久化的组件");
            }
            return new Payload(itemId, data, single.isStackable());
        } catch (IOException error) {
            throw new IllegalArgumentException("物品无法编码", error);
        }
    }

    /** 严格还原单件载荷，拒绝多余字节、未知字段丢失和部分解析结果 */
    public static ItemStack decode(HolderLookup.Provider registries, String itemId, byte[] data) {
        if (data.length == 0 || data.length > MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("物品载荷超过限制");
        }
        try {
            var input = new DataInputStream(new ByteArrayInputStream(data));
            CompoundTag tag = NbtIo.read(input, new NbtAccounter(MAX_NBT_HEAP_BYTES, MAX_NBT_DEPTH));
            if (input.available() != 0 || tag.contains("count")) {
                throw new IllegalArgumentException("物品载荷格式无效");
            }
            validateStructure(tag, 1, new int[1]);
            var ops = registries.createSerializationContext(NbtOps.INSTANCE);
            ItemStack stack = requireComplete(ItemStack.SINGLE_ITEM_CODEC.parse(ops, tag));
            if (stack.isEmpty() || stack.getCount() != 1
                    || !BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(itemId)
                    || !encodeTag(registries, stack).equals(tag)) {
                throw new IllegalArgumentException("物品无法完整还原");
            }
            return stack;
        } catch (IOException error) {
            throw new IllegalArgumentException("物品载荷无法读取", error);
        }
    }

    /** 在原生二进制写入前限制树结构，避免深层递归和大量小节点 */
    private static void validateStructure(Tag tag, int depth, int[] nodes) {
        if (depth > MAX_NBT_DEPTH || ++nodes[0] > MAX_NBT_NODES) {
            throw new IllegalArgumentException("物品 NBT 结构超过限制");
        }
        if (tag instanceof CompoundTag compound) {
            for (String key : compound.getAllKeys()) validateStructure(compound.get(key), depth + 1, nodes);
        } else if (tag instanceof ListTag list) {
            for (Tag child : list) validateStructure(child, depth + 1, nodes);
        }
    }

    /** 仅接受完整成功，不使用 resultOrPartial 资产降级路径 */
    private static <T> T requireComplete(DataResult<T> result) {
        if (result.error().isPresent()) throw new IllegalArgumentException("物品组件编码或还原失败");
        return result.result().orElseThrow(() -> new IllegalArgumentException("物品组件结果为空"));
    }

    /** 由当前世界注册表编码，根节点必须是单件 Compound */
    private static CompoundTag encodeTag(HolderLookup.Provider registries, ItemStack stack) {
        Tag tag = requireComplete(ItemStack.SINGLE_ITEM_CODEC.encodeStart(
                registries.createSerializationContext(NbtOps.INSTANCE), stack));
        if (!(tag instanceof CompoundTag compound)) throw new IllegalArgumentException("物品根节点无效");
        return compound;
    }
}
