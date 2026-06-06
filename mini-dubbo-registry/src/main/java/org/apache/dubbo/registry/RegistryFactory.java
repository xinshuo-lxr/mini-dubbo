package org.apache.dubbo.registry;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;

/**
 * 注册中心工厂 — 根据 URL 的 protocol 选择具体的 Registry 实现。
 *
 * 为什么需要工厂？
 * Registry 的实现类（如 ZookeeperRegistry）需要 URL 作为构造参数，
 * 但 SPI 的 getExtension(name) 只接受字符串名字，无法传 URL。
 * 工厂模式解决了"SPI 扩展点需要构造参数"的问题。
 *
 * @SPI 不指定默认扩展名（与 Dubbo 一致），必须通过 @Adaptive 动态选择。
 *
 * @Adaptive({"protocol"}) 表示：
 *   运行时从方法参数的 URL 中读取 protocol 字段，
 *   用这个值去 SPI 选择具体工厂实现。
 *   URL.protocol = "zookeeper" → ZookeeperRegistryFactory
 *
 * 对应 Dubbo 源码：org.apache.dubbo.registry.RegistryFactory
 */
@SPI
public interface RegistryFactory {

    /**
     * 根据 URL 创建 Registry 实例
     *
     * @Adaptive 会从 url.getProtocol() 读取扩展名
     * 例如 url = zookeeper://localhost:2181 → 选择 ZookeeperRegistryFactory
     */
    @Adaptive({"protocol"})
    Registry getRegistry(URL url);
}
