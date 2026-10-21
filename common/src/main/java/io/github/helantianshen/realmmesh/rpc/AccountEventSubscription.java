// 账户通知连接：保存单条待处理事件，并将网络状态交给服务器线程读取

package io.github.helantianshen.realmmesh.rpc;

import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.Status;
import realmmesh.auth.v1.Auth;

import java.util.concurrent.atomic.AtomicReference;

/** 单次订阅的网络句柄，不执行游戏操作，也不自行重连 */
public final class AccountEventSubscription implements AutoCloseable {
    private final ClientCall<Auth.SubscribeAccountEventsRequest, Auth.AccountEvent> call;
    private final AtomicReference<Auth.AccountEvent> pending = new AtomicReference<>();
    private volatile boolean ready;
    private volatile boolean closed;
    private volatile Status status;

    /** 发起订阅，不设置整个流的期限；响应头等待上限由连接生命周期负责 */
    AccountEventSubscription(ClientCall<Auth.SubscribeAccountEventsRequest, Auth.AccountEvent> call, Metadata headers) {
        this.call = call;
        call.start(new ClientCall.Listener<>() {
            /** 中心在登记订阅后发送响应头，此时才算订阅就绪 */
            @Override
            public void onHeaders(Metadata headers) {
                ready = true;
            }

            /** 网络线程只交付消息，不接触玩家或服务器状态 */
            @Override
            public void onMessage(Auth.AccountEvent event) {
                if (!closed) pending.set(event);
            }

            /** 保留结束状态供服务器线程决定重连，不记录远端错误正文 */
            @Override
            public void onClose(Status ended, Metadata trailers) {
                status = ended;
            }
        }, headers);

        // 每次只允许接收一条，服务器线程取走后才授予下一条接收额度
        call.request(1);
        call.sendMessage(Auth.SubscribeAccountEventsRequest.getDefaultInstance());
        call.halfClose();
    }

    /** 返回中心是否已确认订阅，不将 Channel 建立误认为订阅就绪 */
    public boolean ready() {
        return ready;
    }

    /** 返回结束状态，null 表示流尚未结束 */
    public Status status() {
        return status;
    }

    /** 非阻塞取出一条事件，允许处理流结束前已经收到的最后一条消息 */
    public Auth.AccountEvent poll() {
        if (closed) return null;
        var event = pending.getAndSet(null);
        if (event != null && status == null) call.request(1);
        return event;
    }

    /** 取消当前流并丢弃未处理事件，关闭后迟到回调不能交付给游戏线程 */
    @Override
    public void close() {
        closed = true;
        pending.set(null);
        call.cancel("账户通知订阅已关闭", null);
    }
}
