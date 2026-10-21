// 玩家登录期限：按进服时的配置固定截止时间，并计算一次性提醒

package io.github.helantianshen.realmmesh.neoforge.auth;

import java.util.concurrent.TimeUnit;

/** 使用单调时钟，配置刷新及认证重试均不改变本次连接的期限 */
public final class LoginDeadline {
    private final int timeoutSeconds;
    private final long deadlineNanos;
    private boolean remindedThirty;
    private boolean remindedTen;

    /** 将正整数秒数转换为纳秒，int32 秒数不会超过 long 的纳秒范围 */
    public LoginDeadline(int timeoutSeconds, long nowNanos) {
        if (timeoutSeconds <= 0) throw new IllegalArgumentException("登录时限必须为正数");
        this.timeoutSeconds = timeoutSeconds;
        this.deadlineNanos = nowNanos + TimeUnit.SECONDS.toNanos(timeoutSeconds);
    }

    /** 使用时间差比较，允许 nanoTime 的计数器跨越 long 边界 */
    public boolean expired(long nowNanos) {
        return nowNanos - deadlineNanos >= 0;
    }

    /** 返回本次需要提示的剩余秒数，0 表示不提醒；跳过因卡顿而错过的较早节点 */
    public int reminder(long nowNanos) {
        long remaining = deadlineNanos - nowNanos;
        if (remaining <= 0) return 0;
        int seconds = (int) ((remaining - 1) / TimeUnit.SECONDS.toNanos(1) + 1);
        if (timeoutSeconds > 10 && seconds <= 10 && !remindedTen) {
            remindedTen = true;
            remindedThirty = true;
            return seconds;
        }
        if (timeoutSeconds > 30 && seconds <= 30 && !remindedThirty) {
            remindedThirty = true;
            return seconds;
        }
        return 0;
    }
}
