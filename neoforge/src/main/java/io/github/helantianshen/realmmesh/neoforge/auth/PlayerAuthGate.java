// 玩家门禁：服务端认证前的交互限制、临时免伤与位置固定

package io.github.helantianshen.realmmesh.neoforge.auth;

import io.github.helantianshen.realmmesh.RealmMesh;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** 未知状态与认证处理中均不放行，管理员没有门禁豁免 */
@EventBusSubscriber(modid = RealmMesh.MOD_ID)
public final class PlayerAuthGate {
    /** 事件入口与 Mixin 共用静态门禁判断 */
    private PlayerAuthGate() {}

    /** 只对真实服务端玩家生效，不把客户端状态作为服务端依据 */
    public static boolean blocked(Entity entity) {
        return entity instanceof ServerPlayer player && !PlayerAuthentication.isAuthenticated(player);
    }

    /** 拒绝操作并限频提示，不改写玩家的游戏模式或权限 */
    public static boolean deny(ServerPlayer player) {
        if (!blocked(player)) return false;
        PlayerAuthentication.remindBlocked(player);
        return true;
    }

    /** 在原版玩家逻辑前检查超时并清除推动速度，不恢复生命或饥饿数值 */
    @SubscribeEvent
    public static void beforeTick(PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerAuthentication.tick(player);
            if (blocked(player)) {
                player.setDeltaMovement(Vec3.ZERO);
                player.stopUsingItem();
                player.stopRiding();
            }
        }
    }

    /** 修正外部传送导致的位置变化，保持视角；关闭非玩家背包容器 */
    @SubscribeEvent
    public static void afterTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && blocked(player)) {
            var anchor = PlayerAuthentication.anchor(player);
            if (player.position().distanceToSqr(anchor) > 0.000001) {
                player.connection.teleport(anchor.x, anchor.y, anchor.z, player.getYRot(), player.getXRot());
            }
            player.setDeltaMovement(Vec3.ZERO);
            if (player.containerMenu != player.inventoryMenu) player.closeContainer();
        }
    }

    /** 阻止未登录玩家受到伤害，也阻止其已有投射物造成伤害 */
    @SubscribeEvent
    public static void damage(LivingIncomingDamageEvent event) {
        if (blocked(event.getEntity()) || blocked(event.getSource().getEntity())) event.setCanceled(true);
    }

    /** 拒绝近战攻击，在造成伤害与武器消耗前停止 */
    @SubscribeEvent
    public static void attack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && deny(player)) event.setCanceled(true);
    }

    /** 对具体交互事件执行统一判断，NeoForge 不允许订阅抽象事件基类 */
    private static void cancelInteraction(PlayerInteractEvent event, ICancellableEvent cancellable) {
        if (event.getEntity() instanceof ServerPlayer player && deny(player)) cancellable.setCanceled(true);
    }

    /** 阻止方块使用，包括开箱、按钮和放置物品 */
    @SubscribeEvent
    public static void useBlock(PlayerInteractEvent.RightClickBlock event) {
        cancelInteraction(event, event);
    }

    /** 阻止空中使用物品，包括食物、弓和投掷物 */
    @SubscribeEvent
    public static void useItem(PlayerInteractEvent.RightClickItem event) {
        cancelInteraction(event, event);
    }

    /** 阻止左键方块交互及开始挖掘 */
    @SubscribeEvent
    public static void hitBlock(PlayerInteractEvent.LeftClickBlock event) {
        cancelInteraction(event, event);
    }

    /** 阻止实体交易、骑乘和其他普通交互 */
    @SubscribeEvent
    public static void useEntity(PlayerInteractEvent.EntityInteract event) {
        cancelInteraction(event, event);
    }

    /** 阻止盔甲架等按命中部位处理的实体交互 */
    @SubscribeEvent
    public static void useEntityPart(PlayerInteractEvent.EntityInteractSpecific event) {
        cancelInteraction(event, event);
    }

    /** 阻止方块破坏，涵盖事件路径中的其他调用者 */
    @SubscribeEvent
    public static void breakBlock(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && deny(player)) event.setCanceled(true);
    }

    /** 拒绝拾取地面物品，不移除物品实体 */
    @SubscribeEvent
    public static void pickup(ItemEntityPickupEvent.Pre event) {
        if (blocked(event.getPlayer())) event.setCanPickup(TriState.FALSE);
    }

    /** 拒绝经验球拾取，避免等待登录期间改变经验和修补装备 */
    @SubscribeEvent
    public static void pickupExperience(PlayerXpEvent.PickupXp event) {
        if (blocked(event.getEntity())) event.setCanceled(true);
    }

    /** 阻止骑乘，允许门禁主动将玩家移下载具 */
    @SubscribeEvent
    public static void mount(EntityMountEvent event) {
        if (event.isMounting() && blocked(event.getEntityMounting())) event.setCanceled(true);
    }

    /** 未登录期间不允许通过传送门或其他事件路径切换维度 */
    @SubscribeEvent
    public static void dimension(EntityTravelToDimensionEvent event) {
        if (blocked(event.getEntity())) event.setCanceled(true);
    }

    /** 阻止珍珠等事件路径的传送；门禁位置修正使用连接的 teleport */
    @SubscribeEvent
    public static void teleport(EntityTeleportEvent event) {
        if (blocked(event.getEntity())) event.setCanceled(true);
    }

    /** 在原版聊天签名状态处理后取消广播，不破坏后续签名链 */
    @SubscribeEvent
    public static void chat(ServerChatEvent event) {
        if (deny(event.getPlayer())) event.setCanceled(true);
    }

    /** 只允许解析为 realmmesh login/register 的直接命令，拒绝 execute 等包装命令 */
    @SubscribeEvent
    public static void command(CommandEvent event) {
        var context = event.getParseResults().getContext();
        if (!(context.getSource().getEntity() instanceof ServerPlayer player) || !blocked(player)) return;
        var nodes = context.getNodes();
        boolean authCommand = nodes.size() >= 2
                && nodes.get(0).getNode().getName().equals("realmmesh")
                && (nodes.get(1).getNode().getName().equals("login") || nodes.get(1).getNode().getName().equals("register"));
        if (!authCommand) {
            PlayerAuthentication.remindBlocked(player);
            event.setCanceled(true);
        }
    }
}
