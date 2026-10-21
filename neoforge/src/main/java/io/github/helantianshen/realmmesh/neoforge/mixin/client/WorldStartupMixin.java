// 内置服启动失败：只拦截中心检查失败，退出世界加载并保留客户端

package io.github.helantianshen.realmmesh.neoforge.mixin.client;

import io.github.helantianshen.realmmesh.rpc.CenterStartupException;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

/** 内置服将启动异常交给客户端延迟崩溃处理，此处仅接管已知的中心启动失败 */
@Mixin(Minecraft.class)
public abstract class WorldStartupMixin {
    @Shadow private Supplier<CrashReport> delayedCrash;

    /** 在世界加载等待循环处理崩溃前，关闭失败的内置服并展示安全原因 */
    @Inject(method = "doWorldLoad", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;handleDelayedCrash()V"), cancellable = true)
    private void realmmesh$handleStartupFailure(LevelStorageSource.LevelStorageAccess storage,
            PackRepository packs, WorldStem worldStem, boolean newWorld, CallbackInfo callback) {
        if (delayedCrash == null) return;
        Throwable cause = delayedCrash.get().getException();
        while (cause != null && !(cause instanceof CenterStartupException)) cause = cause.getCause();
        if (!(cause instanceof CenterStartupException failure)) return;

        // 清除本次已处理报告，避免回到主循环后再次触发客户端退出
        delayedCrash = null;
        Minecraft minecraft = (Minecraft) (Object) this;
        minecraft.getProfiler().pop();
        minecraft.disconnect();
        minecraft.setScreen(new DisconnectedScreen(new TitleScreen(),
                Component.literal("RealmMesh 启动失败"), Component.literal(failure.getMessage())));
        callback.cancel();
    }
}
