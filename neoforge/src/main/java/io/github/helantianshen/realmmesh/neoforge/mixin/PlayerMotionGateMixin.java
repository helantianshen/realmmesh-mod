// 玩家门禁物理补充：阻止服务端物理位移和推动累积，不修改无敌或飞行能力

package io.github.helantianshen.realmmesh.neoforge.mixin;

import io.github.helantianshen.realmmesh.neoforge.auth.PlayerAuthGate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 只拦截未登录 ServerPlayer，其他实体物理逻辑保持原样 */
@Mixin(Entity.class)
public abstract class PlayerMotionGateMixin {
    /** 阻止重力、水流、碰撞和活塞等路径造成位移 */
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void realmmesh$move(MoverType type, Vec3 movement, CallbackInfo callback) {
        if (PlayerAuthGate.blocked((Entity) (Object) this)) callback.cancel();
    }

    /** 阻止推动速度累积，避免登录后突然释放等待期间的推动 */
    @Inject(method = "push(DDD)V", at = @At("HEAD"), cancellable = true)
    private void realmmesh$push(double x, double y, double z, CallbackInfo callback) {
        if (PlayerAuthGate.blocked((Entity) (Object) this)) callback.cancel();
    }
}
