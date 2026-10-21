// 玩家门禁饥饿补充：暂停原版饥饿计算与自然回血，不回填状态

package io.github.helantianshen.realmmesh.neoforge.mixin;

import io.github.helantianshen.realmmesh.neoforge.auth.PlayerAuthGate;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 食物计时器保持原值，认证完成后从原进度继续 */
@Mixin(FoodData.class)
public abstract class PlayerFoodGateMixin {
    /** 暂停饥饿消耗、饱食恢复与饥饿伤害，不停止玩家的效果计时 */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void realmmesh$foodTick(Player player, CallbackInfo callback) {
        if (PlayerAuthGate.blocked(player)) callback.cancel();
    }
}
