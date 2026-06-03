package org.apache.dubbo.rpc;

/**
 * 调用结果接口。
 *
 * 对应 Dubbo 源码：org.apache.dubbo.rpc.Result
 */
public interface Result {

    /** 获取返回值 */
    Object getValue();

    /** 获取异常（如果有） */
    Throwable getException();

    /** 是否有异常 */
    boolean hasException();
}
