package org.apache.dubbo.rpc;

/**
 * Exporter 接口 — 代表一个已暴露的服务。
 *
 * Provider 调用 Protocol.export() 后得到 Exporter，
 * 通过它可以取消暴露（unexport）。
 *
 * 对应 Dubbo 源码：org.apache.dubbo.rpc.Exporter
 */
public interface Exporter<T> {

    /** 获取对应的 Invoker */
    Invoker<T> getInvoker();

    /** 取消暴露 */
    void unexport();
}
