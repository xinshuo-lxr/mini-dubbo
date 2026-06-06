package org.apache.dubbo.config;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.registry.integration.RegistryDirectory;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.filter.FilterChainBuilder;
import org.apache.dubbo.rpc.protocol.Protocol;
import org.apache.dubbo.rpc.proxy.ProxyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务引用配置 — Consumer 端核心。
 *
 * 工作流程：
 *   1. 构建 Consumer URL
 *   2. 创建 RegistryDirectory（动态目录）
 *   3. Registry.subscribe() 订阅 ZK，notify 时自动创建 Invoker
 *   4. Cluster.join(directory) 包装成 ClusterInvoker（带重试/快速失败）
 *   5. FilterChainBuilder 包装 ClusterFilter 链
 *   6. ProxyFactory.getProxy() 生成代理对象
 *
 * 对应 Dubbo 源码：org.apache.dubbo.config.ReferenceConfig
 */
public class ReferenceConfig<T> {

    private static final Logger logger = LoggerFactory.getLogger(ReferenceConfig.class);

    /** 服务接口 */
    private Class<T> interfaceClass;

    /** 应用配置 */
    private ApplicationConfig applicationConfig;

    /** 注册中心配置 */
    private RegistryConfig registryConfig;

    /** 服务分组（可选） */
    private String group;

    /** 服务版本（可选） */
    private String version;

    /** 超时时间（毫秒） */
    private int timeout = 3000;

    /** 重试次数 */
    private int retries = 2;

    /** 生成的代理对象 */
    private volatile T proxy;

    public ReferenceConfig() {}

    public ReferenceConfig(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    /**
     * 获取代理对象 — 懒加载，首次调用时初始化
     *
     * 对应 Dubbo 源码：ReferenceConfig.get()
     */
    public T get() {
        if (proxy == null) {
            synchronized (this) {
                if (proxy == null) {
                    proxy = createProxy();
                }
            }
        }
        return proxy;
    }

    /**
     * 创建代理对象 — 整个 Consumer 端初始化流程
     *
     * 对应 Dubbo 源码：ReferenceConfig.createProxy()
     */
    @SuppressWarnings("unchecked")
    private T createProxy() {
        // 1. 构建 Consumer URL
        URL consumerUrl = buildConsumerUrl();

        // 2. 创建 RegistryDirectory（动态目录）
        RegistryDirectory<T> directory = new RegistryDirectory<>(interfaceClass, consumerUrl);

        // 3. 注册到 ZK + 订阅 Provider 变化
        if (registryConfig != null) {
            URL registryUrl = buildRegistryUrl();
            // 通过 RegistryFactory 创建 Registry（@Adaptive 从 URL.protocol 选择工厂）
            RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class)
                    .getAdaptiveExtension();
            Registry registry = registryFactory.getRegistry(registryUrl);

            // subscribe 会触发 notify → RegistryDirectory 创建 Invoker
            registry.subscribe(consumerUrl, directory);
            logger.info("Subscribed to registry for " + interfaceClass.getName());
        }

        // 4. Cluster.join()：把 Directory 包装成 ClusterInvoker
        Cluster cluster = ExtensionLoader.getExtensionLoader(Cluster.class)
                .getExtension("failover");
        Invoker<T> invoker = cluster.join(directory);
        logger.info("Cluster invoker created: " + invoker.getClass().getSimpleName());

        // 5. 包装 ClusterFilter 链
        invoker = FilterChainBuilder.buildClusterFilterChain(invoker, consumerUrl);

        // 6. ProxyFactory.getProxy()：Invoker → 代理对象
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class)
                .getExtension("jdk");
        T proxy = proxyFactory.getProxy(invoker);
        logger.info("Proxy created for " + interfaceClass.getName());

        return proxy;
    }

    /**
     * 构建 Consumer URL
     *
     * 格式：consumer://192.168.1.100/org.apache.dubbo.api.demo.DemoService?application=xxx&check=false
     */
    private URL buildConsumerUrl() {
        Map<String, String> params = new HashMap<>();

        if (applicationConfig != null) {
            params.put("application", applicationConfig.getName());
        }

        params.put("interface", interfaceClass.getName());
        params.put("check", "false");
        params.put("timeout", String.valueOf(timeout));
        params.put("retries", String.valueOf(retries));

        if (group != null) {
            params.put("group", group);
        }
        if (version != null) {
            params.put("version", version);
        }

        return new URL("consumer", NetUtils.getLocalHost(), 0, interfaceClass.getName(), params);
    }

    /**
     * 构建注册中心 URL
     */
    private URL buildRegistryUrl() {
        String protocol = registryConfig.getProtocol();
        String address = registryConfig.getAddress();
        int port = 2181;
        String host = address;

        int backslash = address.indexOf("//");
        int lastColon = address.lastIndexOf(":");
        if (backslash > 0 && lastColon > 0) {
            host = address.substring(backslash + 2, lastColon);
            port = Integer.parseInt(address.substring(lastColon + 1));
        }

        return new URL(protocol, host, port, "", new HashMap<>());
    }

    // ==================== getter/setter ====================

    public Class<T> getInterfaceClass() { return interfaceClass; }
    public void setInterfaceClass(Class<T> interfaceClass) { this.interfaceClass = interfaceClass; }

    public ApplicationConfig getApplicationConfig() { return applicationConfig; }
    public void setApplicationConfig(ApplicationConfig applicationConfig) { this.applicationConfig = applicationConfig; }

    public RegistryConfig getRegistryConfig() { return registryConfig; }
    public void setRegistryConfig(RegistryConfig registryConfig) { this.registryConfig = registryConfig; }

    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public int getTimeout() { return timeout; }
    public void setTimeout(int timeout) { this.timeout = timeout; }

    public int getRetries() { return retries; }
    public void setRetries(int retries) { this.retries = retries; }
}
