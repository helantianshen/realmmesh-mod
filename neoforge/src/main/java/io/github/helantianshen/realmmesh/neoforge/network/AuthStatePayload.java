// 门禁状态同步：中心凭据和玩家密码均不进入 Minecraft 自定义消息

package io.github.helantianshen.realmmesh.neoforge.network;

import io.github.helantianshen.realmmesh.RealmMesh;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** 单向同步登录布尔值，客户端不能通过此消息改变服务端认证状态 */
public record AuthStatePayload(boolean authenticated) implements CustomPacketPayload {
    public static final Type<AuthStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RealmMesh.MOD_ID, "auth_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AuthStatePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, AuthStatePayload::authenticated, AuthStatePayload::new);

    /** 仅保存客户端展示状态，不被服务端门禁读取 */
    public static boolean clientAuthenticated;

    /** 注册仅由服务器发送的消息，处理器由框架安排到客户端主线程 */
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, CODEC,
                (payload, context) -> clientAuthenticated = payload.authenticated());
    }

    /** 返回固定消息类型，编码时只携带布尔字段 */
    @Override
    public Type<AuthStatePayload> type() {
        return TYPE;
    }

    /** 将当前连接的认证结果发送给对应玩家，不广播给其他玩家 */
    public static void send(ServerPlayer player, boolean authenticated) {
        PacketDistributor.sendToPlayer(player, new AuthStatePayload(authenticated));
    }
}
