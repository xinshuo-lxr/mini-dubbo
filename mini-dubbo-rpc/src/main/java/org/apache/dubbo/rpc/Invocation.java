package org.apache.dubbo.rpc;

import java.util.Map;

/**
 * Invocation 接口 — 流经 Invoker 链的数据载体。
 *
 * 如果 Invoker 是管道，那 Invocation 就是在管道中流动的水。
 * 包含：方法名、参数、attachments（会网络传输）、attributes（仅本地使用）
 *
 * 对应 Dubbo 源码：org.apache.dubbo.rpc.Invocation
 */
public interface Invocation {

    /** 方法名（如 "sayHello"） */
    String getMethodName();

    /** 服务名（如 "org.apache.dubbo.api.demo.DemoService"） */
    String getServiceName();

    /** 参数类型 */
    Class<?>[] getParameterTypes();

    /** 参数值 */
    Object[] getArguments();

    /**
     * 附件 — 会随 RPC 请求发送到 Provider。
     * 包含 path、version、timeout、token 等。
     */
    Map<String, Object> getAttachments();

    /**
     * 属性 — 仅本地使用，不会网络传输。
     * 用于在同一调用链的不同层之间传递临时数据。
     */
    Map<Object, Object> getAttributes();

    /** 获取关联的 Invoker（反向引用） */
    Invoker<?> getInvoker();

    /** 设置关联的 Invoker */
    void setInvoker(Invoker<?> invoker);
}
