// 玩家门禁协议补充：拦截缺少统一可取消事件的移动、背包和客户端管理操作

package io.github.helantianshen.realmmesh.neoforge.mixin;

import io.github.helantianshen.realmmesh.neoforge.auth.PlayerAuthentication;
import io.github.helantianshen.realmmesh.neoforge.auth.PlayerAuthGate;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 所有拦截都在原版切回服务器线程之后执行，保留传送确认及连接维护包 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PlayerPacketGateMixin {
    @Shadow public ServerPlayer player;
    private static final String SERVER_THREAD = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V";

    /** 允许有限角度输入，拒绝坐标变化并发送原位置纠正 */
    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target = SERVER_THREAD, shift = At.Shift.AFTER), cancellable = true)
    private void realmmesh$movement(ServerboundMovePlayerPacket packet, CallbackInfo callback) {
        if (!PlayerAuthGate.blocked(player)) return;
        float yaw = packet.getYRot(player.getYRot());
        float pitch = packet.getXRot(player.getXRot());
        if (Float.isFinite(yaw) && Float.isFinite(pitch)) {
            player.setYRot(yaw);
            player.setXRot(pitch);
        }
        var anchor = PlayerAuthentication.anchor(player);
        if (packet.getX(anchor.x) != anchor.x || packet.getY(anchor.y) != anchor.y || packet.getZ(anchor.z) != anchor.z) {
            player.connection.teleport(anchor.x, anchor.y, anchor.z, player.getYRot(), player.getXRot());
        }
        callback.cancel();
    }

    /** 阻止载具控制、飞行切换和玩家动作，不消费传送确认包 */
    @Inject(method = {"handlePlayerInput", "handleMoveVehicle", "handlePaddleBoat", "handlePlayerAbilities",
            "handlePlayerCommand", "handleTeleportToEntityPacket"},
            at = @At(value = "INVOKE", target = SERVER_THREAD, shift = At.Shift.AFTER), cancellable = true)
    private void realmmesh$control(CallbackInfo callback) {
        if (PlayerAuthGate.deny(player)) callback.cancel();
    }

    /** 丢弃与副手交换缺少统一前置事件，拒绝后同步服务端背包以撤销客户端预测 */
    @Inject(method = "handlePlayerAction", at = @At(value = "INVOKE", target = SERVER_THREAD, shift = At.Shift.AFTER), cancellable = true)
    private void realmmesh$itemAction(ServerboundPlayerActionPacket packet, CallbackInfo callback) {
        switch (packet.getAction()) {
            case DROP_ITEM, DROP_ALL_ITEMS, SWAP_ITEM_WITH_OFFHAND -> {
                if (PlayerAuthGate.deny(player)) {
                    player.containerMenu.sendAllDataToRemote();
                    callback.cancel();
                }
            }
            default -> { /* 方块破坏走 NeoForge 交互事件，保留原版预测确认流程 */ }
        }
    }

    /** 拒绝容器、合成与创造模式背包修改，校正客户端预测结果 */
    @Inject(method = {"handleContainerClick", "handleContainerButtonClick", "handlePlaceRecipe", "handleSetCreativeModeSlot",
            "handlePickItem", "handleSetCarriedItem", "handleSelectTrade", "handleRenameItem", "handleSetBeaconPacket",
            "handleContainerSlotStateChanged", "handleEditBook"},
            at = @At(value = "INVOKE", target = SERVER_THREAD, shift = At.Shift.AFTER), cancellable = true)
    private void realmmesh$inventory(CallbackInfo callback) {
        if (PlayerAuthGate.deny(player)) {
            player.containerMenu.sendAllDataToRemote();
            player.connection.send(new ClientboundSetCarriedItemPacket(player.getInventory().selected));
            callback.cancel();
        }
    }

    /** 即使玩家有管理员权限，也不能在未登录时发送客户端管理修改 */
    @Inject(method = {"handleSetCommandBlock", "handleSetCommandMinecart", "handleSetStructureBlock", "handleSetJigsawBlock",
            "handleJigsawGenerate", "handleSignUpdate", "handleChangeDifficulty", "handleLockDifficulty"},
            at = @At(value = "INVOKE", target = SERVER_THREAD, shift = At.Shift.AFTER), cancellable = true)
    private void realmmesh$administration(CallbackInfo callback) {
        if (PlayerAuthGate.deny(player)) callback.cancel();
    }
}
