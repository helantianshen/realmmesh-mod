// 玩家门禁拾取补充：箭和三叉戟不经过普通物品拾取事件

package io.github.helantianshen.realmmesh.neoforge.mixin;

import io.github.helantianshen.realmmesh.neoforge.auth.PlayerAuthGate;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 保留投射物，认证完成后再按原版规则拾取 */
@Mixin(AbstractArrow.class)
public abstract class ProjectilePickupGateMixin {
    /** 在尝试写入背包之前拒绝拾取，涵盖三叉戟调用父类的路径 */
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void realmmesh$pickup(Player player, CallbackInfo callback) {
        if (PlayerAuthGate.blocked(player)) callback.cancel();
    }
}
