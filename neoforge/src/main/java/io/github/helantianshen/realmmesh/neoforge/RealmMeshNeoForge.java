package io.github.helantianshen.realmmesh.neoforge;

import io.github.helantianshen.realmmesh.RealmMesh;
import io.github.helantianshen.realmmesh.neoforge.config.NeoForgeRpcConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(RealmMesh.MOD_ID)
public final class RealmMeshNeoForge {
    public RealmMeshNeoForge(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, NeoForgeRpcConfig.SPEC, NeoForgeRpcConfig.FILE_NAME);
        RealmMesh.initialize();
    }
}
