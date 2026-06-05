package org.apache.dubbo.registry.integration;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.filter.FilterChainBuilder;
import org.apache.dubbo.rpc.protocol.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态目录 — 订阅注册中心，动态维护 Invoker 列表。
 *
 * 工作流程：
 * 1. subscribe() 时向注册中心注册 Watcher
 * 2. 注册中心推送 Provider 列表 → notify() 被调用
 * 3. notify() 中把每个 Provider URL 转成 Invoker（通过 Protocol.refer()）
 * 4. 用新列表替换旧列表，销毁多余的 Invoker
 *
 * 实现了两个接口：
 * - Directory<T>：提供 list() 方法给 ClusterInvoker 获取 Invoker 列表
 * - NotifyListener：提供 notify() 方法给注册中心回调
 *
 * 对应 Dubbo 源码：org.apache.dubbo.registry.integration.RegistryDirectory
 */
public class RegistryDirectory<T> implements Directory<T>, NotifyListener {

    private static final Logger logger = LoggerFactory.getLogger(RegistryDirectory.class);

    private final Class<T> serviceType;
    private final URL consumerUrl;

    /** 当前可用的 Invoker 列表（由 notify 更新） */
    private volatile List<Invoker<T>> invokers = new ArrayList<>();

    /** URL → Invoker 缓存（避免重复创建） */
    private final Map<String, Invoker<T>> urlInvokerMap = new ConcurrentHashMap<>();

    private volatile boolean destroyed;

    public RegistryDirectory(Class<T> serviceType, URL consumerUrl) {
        this.serviceType = serviceType;
        this.consumerUrl = consumerUrl;
    }

    /**
     * 注册中心回调 — 收到最新的 Provider URL 列表
     *
     * 这是整个 Consumer 端最核心的方法之一：
     * 把 Provider URL 列表转成 Invoker 列表
     */
    @Override
    public void notify(List<URL> urls) {
        if (destroyed || urls == null) {
            return;
        }

        logger.info("RegistryDirectory notified, new provider count: " + urls.size());

        Map<String, Invoker<T>> newUrlInvokerMap = new ConcurrentHashMap<>();
        List<Invoker<T>> newInvokers = new ArrayList<>();

        for (URL url : urls) {
            String key = url.toFullString();

            // 复用已有的 Invoker
            Invoker<T> invoker = urlInvokerMap.remove(key);
            if (invoker == null) {
                // 新的 Provider，创建 Invoker
                invoker = createInvoker(url);
            }
            newUrlInvokerMap.put(key, invoker);
            newInvokers.add(invoker);
        }

        // 销毁不再需要的 Invoker（Provider 下线了）
        for (Invoker<T> oldInvoker : urlInvokerMap.values()) {
            oldInvoker.destroy();
        }

        // 替换
        this.urlInvokerMap.putAll(newUrlInvokerMap);
        this.invokers = newInvokers;

        logger.info("RegistryDirectory updated, active invokers: " + newInvokers.size());
    }

    /**
     * 从 Provider URL 创建 Invoker
     *
     * 1. 合并消费者参数到 Provider URL
     * 2. Protocol.refer() 创建 DubboInvoker
     * 3. FilterChainBuilder 包装 Filter 链
     */
    private Invoker<T> createInvoker(URL providerUrl) {
        // 合并消费者参数（timeout、retries 等以消费者为准）
        URL url = mergeUrl(providerUrl);

        // 通过 SPI 获取 Protocol，调用 refer() 创建 Invoker
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class)
                .getExtension(url.getProtocol());
        Invoker<T> invoker = protocol.refer(serviceType, url);

        // 包装 Filter 链
        invoker = FilterChainBuilder.buildInvokerChain(invoker, url);

        return invoker;
    }

    /**
     * 合并消费者参数到 Provider URL
     * 规则：消费者参数覆盖 Provider 参数（group/version 除外）
     */
    private URL mergeUrl(URL providerUrl) {
        Map<String, String> consumerParams = consumerUrl.getParameters();
        Map<String, String> providerParams = providerUrl.getParameters();

        Map<String, String> merged = new ConcurrentHashMap<>(providerParams);
        // 消费者参数覆盖 Provider 参数
        for (Map.Entry<String, String> entry : consumerParams.entrySet()) {
            String key = entry.getKey();
            // group 和 version 以 Provider 为准
            if ("group".equals(key) || "version".equals(key)) {
                continue;
            }
            merged.put(key, entry.getValue());
        }
        // 强制 check=false
        merged.put("check", "false");

        return new URL(
                providerUrl.getProtocol(),
                providerUrl.getUsername(),
                providerUrl.getPassword(),
                providerUrl.getHost(),
                providerUrl.getPort(),
                providerUrl.getPath(),
                merged
        );
    }

    @Override
    public List<Invoker<T>> list(Invocation invocation) throws RpcException {
        if (destroyed) {
            throw new RpcException("Directory is destroyed");
        }
        return invokers;
    }

    @Override
    public List<Invoker<T>> getAllInvokers() {
        return Collections.unmodifiableList(invokers);
    }

    @Override
    public URL getUrl() { return consumerUrl; }

    @Override
    public boolean isAvailable() { return !destroyed && !invokers.isEmpty(); }

    @Override
    public void destroy() {
        destroyed = true;
        for (Invoker<T> invoker : invokers) {
            invoker.destroy();
        }
        invokers.clear();
        urlInvokerMap.clear();
    }
}
