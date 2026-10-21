// NeoForge 加载入口：注册本机配置与客户端门禁状态消息

package io.github.helantianshen.realmmesh.neoforge;

import io.github.helantianshen.realmmesh.RealmMesh;
import io.github.helantianshen.realmmesh.neoforge.network.AuthStatePayload;
import net.neoforged.bus.api.IEventBus;
import io.github.helantianshen.realmmesh.neoforge.config.NeoForgeRpcConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(RealmMesh.MOD_ID)
public final class RealmMeshNeoForge {
    /** 在模组初始化时注册配置和单向状态消息，不建立中心连接 */
    public RealmMeshNeoForge(ModContainer container, IEventBus modBus) {
        modBus.addListener(AuthStatePayload::register);
        container.registerConfig(ModConfig.Type.COMMON, NeoForgeRpcConfig.SPEC, NeoForgeRpcConfig.FILE_NAME);
        RealmMesh.initialize();
    }
}
