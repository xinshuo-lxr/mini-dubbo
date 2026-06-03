package org.apache.dubbo.rpc.protocol;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;

/**
 * Protocol 接口 — 协议扩展点。
 *
 * export(): Provider 端暴露服务（开启网络监听）
 * refer():  Consumer 端引用服务（创建 Invoker）
 *
 * @SPI("dubbo") 表示默认使用 dubbo 协议。
 * @Adaptive 表示运行时根据 URL 的 protocol 参数动态选择实现。
 *
 * 对应 Dubbo 源码：org.apache.dubbo.rpc.Protocol
 */
@SPI("dubbo")
public interface Protocol {

    /**
     * 暴露服务（Provider 端）
     * URL 协议 = "dubbo" → DubboProtocol
     * URL 协议 = "registry" → RegistryProtocol
     */
    @Adaptive
    <T> Exporter<T> export(Invoker<T> invoker) throws RpcException;

    /**
     * 引用服务（Consumer 端）
     * 根据 URL 的协议选择对应的 Protocol 实现
     */
    @Adaptive
    <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException;
}
