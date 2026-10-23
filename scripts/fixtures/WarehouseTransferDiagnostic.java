// 仓库协调器断言：验证响应领取、发物授权和明确拒绝分类，不创建游戏玩家

import com.google.protobuf.ByteString;
import io.github.helantianshen.realmmesh.neoforge.warehouse.WarehouseTransfers;
import io.grpc.Status;
import realmmesh.warehouse.v1.Warehouse;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/** 直接检查正式协调器使用的纯状态判定，避免复制另一份实现 */
public final class WarehouseTransferDiagnostic {
    private static int checks;

    /** 创建不含游戏对象的操作数据，覆盖重复与迟到响应边界 */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void main(String[] args) throws Exception {
        Class<?> owner = WarehouseTransfers.class;
        Class<?> transferType = Class.forName(owner.getName() + "$Transfer");
        Class<?> stateType = Class.forName(owner.getName() + "$Transfers");
        Class<? extends Enum> phaseType = (Class<? extends Enum>) Class.forName(owner.getName() + "$Phase");
        Constructor<?> transferConstructor = transferType.getDeclaredConstructor(Warehouse.PlayerIdentity.class,
                long.class, int.class, boolean.class);
        transferConstructor.setAccessible(true);
        Object transfer = transferConstructor.newInstance(Warehouse.PlayerIdentity.newBuilder()
                .setUserId(1).setPlayerName("PlayerOne").build(), 2L, 16, false);
        Constructor<?> stateConstructor = stateType.getDeclaredConstructor();
        stateConstructor.setAccessible(true);
        Object state = stateConstructor.newInstance();
        Object committing = Enum.valueOf(phaseType, "COMMITTING");
        set(transfer, "phase", committing);
        set(transfer, "inventoryId", 3L);
        set(transfer, "itemId", "minecraft:stone");
        ByteString hash = ByteString.copyFrom(new byte[32]);
        set(transfer, "hash", hash);
        String operationId = (String) get(transfer, "operationId");
        Map<String, Object> pending = (Map<String, Object>) get(state, "pending");
        Method owns = method(owner, "ownsResponse", stateType, transferType, phaseType);
        Method authorizes = method(owner, "authorizesWithdrawal", transferType, Warehouse.Operation.class);
        Method explicit = method(owner, "explicitRejection", Throwable.class);

        check(!(boolean) owns.invoke(null, state, transfer, committing), "没有已知在途请求不得领取响应");
        pending.put(operationId, transfer);
        check((boolean) owns.invoke(null, state, transfer, committing), "当前条目与阶段可以领取响应");
        set(transfer, "phase", Enum.valueOf(phaseType, "GIVING"));
        check(!(boolean) owns.invoke(null, state, transfer, committing), "发放开始后重复授权不再领取");
        set(transfer, "phase", committing);
        pending.put(operationId, new Object());
        check(!(boolean) owns.invoke(null, state, transfer, committing), "同编号其他条目不能获得旧响应");
        pending.remove(operationId);
        check(!(boolean) owns.invoke(null, state, transfer, committing), "已完成并清理的操作不得再次发物");

        var authorization = Warehouse.Operation.newBuilder().setOperationId(operationId)
                .setInventoryId(3).setItemId("minecraft:stone").setItemDataHash(hash).setQuantity(16)
                .setState(Warehouse.OperationState.OPERATION_STATE_DEBITED).build();
        check((boolean) authorizes.invoke(null, transfer, authorization), "匹配的已扣库结果可以授权");
        for (var value : new Warehouse.OperationState[]{Warehouse.OperationState.OPERATION_STATE_UNSPECIFIED,
                Warehouse.OperationState.OPERATION_STATE_DEPOSITED, Warehouse.OperationState.OPERATION_STATE_DELIVERED,
                Warehouse.OperationState.OPERATION_STATE_REFUNDED, Warehouse.OperationState.OPERATION_STATE_REJECTED}) {
            check(!(boolean) authorizes.invoke(null, transfer, authorization.toBuilder().setState(value).build()),
                    "非扣库状态不授权：" + value);
        }
        check(!(boolean) authorizes.invoke(null, transfer, authorization.toBuilder().setQuantity(15).build()), "数量不符不授权");
        check(!(boolean) authorizes.invoke(null, transfer, authorization.toBuilder().setInventoryId(4).build()), "库存不符不授权");
        check(!(boolean) authorizes.invoke(null, transfer, authorization.toBuilder().setOperationId("another").build()), "操作不符不授权");
        byte[] otherHash = new byte[32];
        otherHash[0] = 1;
        check(!(boolean) authorizes.invoke(null, transfer, authorization.toBuilder()
                .setItemDataHash(ByteString.copyFrom(otherHash)).build()), "预检查指纹变化不授权");

        for (var status : new Status[]{Status.INVALID_ARGUMENT, Status.FAILED_PRECONDITION,
                Status.NOT_FOUND, Status.RESOURCE_EXHAUSTED}) {
            check((boolean) explicit.invoke(null, status.asRuntimeException()), "固定业务拒绝可归还：" + status.getCode());
        }
        for (var status : new Status[]{Status.DEADLINE_EXCEEDED, Status.UNAVAILABLE, Status.INTERNAL,
                Status.ALREADY_EXISTS, Status.PERMISSION_DENIED, Status.CANCELLED}) {
            check(!(boolean) explicit.invoke(null, status.asRuntimeException()), "未知或编号冲突不能归还：" + status.getCode());
        }
        Method start = method(owner, "startRequest", java.util.function.Supplier.class);
        var successful = (java.util.concurrent.CompletableFuture<?>) start.invoke(null,
                (java.util.function.Supplier<java.util.concurrent.CompletableFuture<String>>)
                        () -> java.util.concurrent.CompletableFuture.completedFuture("ok"));
        check(successful.join().equals("ok"), "正常异步发起保留结果");
        var failed = (java.util.concurrent.CompletableFuture<?>) start.invoke(null,
                (java.util.function.Supplier<java.util.concurrent.CompletableFuture<String>>) () -> {
                    throw new IllegalStateException("同步发起失败");
                });
        check(failed.isCompletedExceptionally(), "同步异常进入统一失败回调");
        var missing = (java.util.concurrent.CompletableFuture<?>) start.invoke(null,
                (java.util.function.Supplier<java.util.concurrent.CompletableFuture<String>>) () -> null);
        check(missing.isCompletedExceptionally(), "空请求结果不能留下未处理暂扣");
        System.out.println("warehouse_transfer_checks_passed=" + checks);
    }

    /** 读取正式类私有状态，仅用于隔离诊断 */
    private static Object get(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    /** 设置纯数据字段，不伪造游戏对象或修改线上状态 */
    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    /** 取得正式判定方法，测试与运行使用同一实现 */
    private static Method method(Class<?> owner, String name, Class<?>... parameters) throws Exception {
        Method method = owner.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method;
    }

    /** 失败立即退出，成功累计断言数 */
    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
        checks++;
    }
}
