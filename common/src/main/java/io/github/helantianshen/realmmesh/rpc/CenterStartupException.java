// 中心启动检查：向加载器传递可安全展示的启动失败原因

package io.github.helantianshen.realmmesh.rpc;

/** 只包含固定诊断文本，不附加可能携带凭据的原始异常 */
public final class CenterStartupException extends RuntimeException {
    /** 保存管理员可读的启动失败说明 */
    public CenterStartupException(String message) {
        super(message);
    }
}
