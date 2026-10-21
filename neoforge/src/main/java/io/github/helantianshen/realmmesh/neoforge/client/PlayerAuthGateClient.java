// 客户端门禁辅助：按服务器同步状态抑制输入与背包界面

package io.github.helantianshen.realmmesh.neoforge.client;

import io.github.helantianshen.realmmesh.RealmMesh;
import io.github.helantianshen.realmmesh.neoforge.network.AuthStatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.InputEvent;

/** 仅在客户端加载，界面限制不能替代服务器校验 */
@EventBusSubscriber(modid = RealmMesh.MOD_ID, value = Dist.CLIENT)
public final class PlayerAuthGateClient {
    /** 通过客户端事件调用 */
    private PlayerAuthGateClient() {}

    /** 断线清除旧状态，下一连接默认等待服务器认证消息 */
    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        AuthStatePayload.clientAuthenticated = false;
    }

    /** 只在玩家处于世界且尚未认证时限制界面 */
    private static boolean blocked() {
        return Minecraft.getInstance().player != null && !AuthStatePayload.clientAuthenticated;
    }

    /** 阻止行走和跳跃输入，视角与聊天输入保持可用 */
    @SubscribeEvent
    public static void movement(MovementInputUpdateEvent event) {
        if (!blocked()) return;
        var input = event.getInput();
        input.forwardImpulse = 0;
        input.leftImpulse = 0;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    /** 阻止背包、创造背包和容器界面，不拦截聊天、暂停及断开连接界面 */
    @SubscribeEvent
    public static void screen(ScreenEvent.Opening event) {
        if (blocked() && event.getNewScreen() instanceof AbstractContainerScreen<?>) event.setCanceled(true);
    }

    /** 禁止本地攻击与使用预测，服务端仍独立拒绝对应操作 */
    @SubscribeEvent
    public static void interaction(InputEvent.InteractionKeyMappingTriggered event) {
        if (blocked()) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }
}
