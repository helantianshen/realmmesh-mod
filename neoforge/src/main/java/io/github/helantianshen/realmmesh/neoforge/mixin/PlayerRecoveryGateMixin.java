// 玩家门禁恢复补充：暂停消耗累积及和平难度的自动恢复

package io.github.helantianshen.realmmesh.neoforge.mixin;

import io.github.helantianshen.realmmesh.neoforge.auth.PlayerAuthGate;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerRecoveryGateMixin {
    /** 不累积等待期间的饥饿消耗，避免认证后一次性扣除 */
    @Inject(method = "causeFoodExhaustion", at = @At("HEAD"), cancellable = true)
    private void realmmesh$exhaustion(float exhaustion, CallbackInfo callback) {
        if (PlayerAuthGate.blocked((Player) (Object) this)) callback.cancel();
    }

    /** 仅在本次玩家自然恢复判断中返回禁用，不修改世界游戏规则 */
    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"))
    private boolean realmmesh$naturalRecovery(GameRules rules, GameRules.Key<GameRules.BooleanValue> key) {
        return !PlayerAuthGate.blocked((Player) (Object) this) && rules.getBoolean(key);
    }
}
