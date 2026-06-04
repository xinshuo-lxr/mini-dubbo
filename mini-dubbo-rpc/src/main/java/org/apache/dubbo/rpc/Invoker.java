package org.apache.dubbo.rpc;

import org.apache.dubbo.common.URL;

/**
 * Invoker 接口 — Dubbo 最核心的抽象。
 *
 * 每一层（Filter、Cluster、Protocol）都实现这个接口，
 * 所以可以像洋葱一样层层嵌套，这就是装饰器模式。
 *
 * 对应 Dubbo 源码：org.apache.dubbo.rpc.Invoker
 */
public interface Invoker<T> {

    /** 获取 URL（携带所有配置信息） */
    URL getUrl();

    /** 获取服务接口类型（如 DemoService.class） */
    Class<T> getInterface();

    /** 执行调用 — 唯一的核心方法 */
    Result invoke(Invocation invocation)  throws RpcException;

    /** 是否可用 */
    boolean isAvailable();

    /** 销毁 */
    void destroy();
}
