// 仓库命令：仅允许当前已认证玩家操作自己的主手和主背包

package io.github.helantianshen.realmmesh.neoforge.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.helantianshen.realmmesh.neoforge.warehouse.WarehouseTransfers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/** 命令只表达意图，真实背包和账户由服务端重新读取 */
public final class WarehouseCommand {
    /** 命令树由 RealmMesh 根命令挂载 */
    private WarehouseCommand() {}

    /** 注册列表、主手存入、按库存 ID 取出、状态及明确失败归还入口 */
    public static LiteralArgumentBuilder<CommandSourceStack> tree() {
        return Commands.literal("warehouse").requires(source -> source.getEntity() instanceof ServerPlayer)
                .then(Commands.literal("list")
                        .executes(context -> {
                            WarehouseTransfers.list((ServerPlayer) context.getSource().getEntity(), 0);
                            return 1;
                        })
                        .then(Commands.argument("afterId", LongArgumentType.longArg(0))
                                .executes(context -> {
                                    WarehouseTransfers.list((ServerPlayer) context.getSource().getEntity(),
                                            LongArgumentType.getLong(context, "afterId"));
                                    return 1;
                                })))
                .then(Commands.literal("deposit")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, WarehouseTransfers.MAX_TRANSFER_AMOUNT))
                                .executes(context -> {
                                    WarehouseTransfers.deposit((ServerPlayer) context.getSource().getEntity(),
                                            IntegerArgumentType.getInteger(context, "amount"));
                                    return 1;
                                })))
                .then(Commands.literal("withdraw")
                        .then(Commands.argument("inventoryId", LongArgumentType.longArg(1))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, WarehouseTransfers.MAX_TRANSFER_AMOUNT))
                                        .executes(context -> {
                                            WarehouseTransfers.withdraw((ServerPlayer) context.getSource().getEntity(),
                                                    LongArgumentType.getLong(context, "inventoryId"),
                                                    IntegerArgumentType.getInteger(context, "amount"));
                                            return 1;
                                        }))))
                .then(Commands.literal("status")
                        .executes(context -> {
                            WarehouseTransfers.status((ServerPlayer) context.getSource().getEntity(), "");
                            return 1;
                        })
                        .then(Commands.argument("operationId", StringArgumentType.word())
                                .executes(context -> {
                                    WarehouseTransfers.status((ServerPlayer) context.getSource().getEntity(),
                                            StringArgumentType.getString(context, "operationId"));
                                    return 1;
                                })))
                .then(Commands.literal("return")
                        .executes(context -> {
                            WarehouseTransfers.returnItems((ServerPlayer) context.getSource().getEntity());
                            return 1;
                        }));
    }
}
