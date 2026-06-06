package org.apache.dubbo.config;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.protocol.Protocol;
import org.apache.dubbo.rpc.proxy.ProxyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务暴露配置 — Provider 端核心。
 *
 * 工作流程：
 *   1. 从 ApplicationConfig / RegistryConfig / ProtocolConfig 汇聚参数
 *   2. 构建 Provider URL
 *   3. ProxyFactory.getInvoker() 把业务对象包装成 Invoker
 *   4. Protocol.export() 启动 NettyServer
 *   5. Registry.register() 注册到 ZK
 *
 * 对应 Dubbo 源码：org.apache.dubbo.config.ServiceConfig
 */
public class ServiceConfig<T> {

    private static final Logger logger = LoggerFactory.getLogger(ServiceConfig.class);

    /** 服务接口 */
    private Class<T> interfaceClass;

    /** 服务实现 */
    private T ref;

    /** 应用配置 */
    private ApplicationConfig applicationConfig;

    /** 注册中心配置 */
    private RegistryConfig registryConfig;

    /** 协议配置 */
    private ProtocolConfig protocolConfig;

    /** 服务分组（可选） */
    private String group;

    /** 服务版本（可选） */
    private String version;

    /** 已暴露的 Exporter */
    private volatile Exporter<T> exporter;

    public ServiceConfig() {}

    public ServiceConfig(Class<T> interfaceClass, T ref) {
        this.interfaceClass = interfaceClass;
        this.ref = ref;
    }

    /**
     * 暴露服务 — 启动 NettyServer + 注册到 ZK
     *
     * 对应 Dubbo 源码：ServiceConfig.export()
     */
    public void export() {
        // 1. 汇聚参数，构建 URL
        URL url = buildUrl();

        // 2. ProxyFactory.getInvoker()：业务对象 → Invoker
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class)
                .getExtension("jdk");
        Invoker<T> invoker = proxyFactory.getInvoker(ref, interfaceClass, url);
        logger.info("Created invoker for " + interfaceClass.getName());

        // 3. Protocol.export()：启动 NettyServer
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class)
                .getExtension(url.getProtocol());
        exporter = protocol.export(invoker);
        logger.info("Exported service: " + url.getServiceKey() + " on port " + url.getPort());

        // 4. Registry.register()：注册到 ZK
        if (registryConfig != null) {
            URL registryUrl = buildRegistryUrl();
            // 通过 RegistryFactory 创建 Registry（@Adaptive 从 URL.protocol 选择工厂）
            RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class)
                    .getAdaptiveExtension();
            Registry registry = registryFactory.getRegistry(registryUrl);
            registry.register(url);
            logger.info("Registered service to registry: " + url.getServiceKey());
        }
    }

    /**
     * 取消暴露
     */
    public void unexport() {
        if (exporter != null) {
            exporter.unexport();
            logger.info("Unexported service: " + interfaceClass.getName());
        }
    }

    /**
     * 构建 Provider URL
     *
     * 格式：dubbo://0.0.0.0:20880/org.apache.dubbo.api.demo.DemoService?application=xxx&group=xxx
     */
    private URL buildUrl() {
        Map<String, String> params = new HashMap<>();

        // 应用名
        if (applicationConfig != null) {
            params.put("application", applicationConfig.getName());
        }

        // 接口名
        params.put("interface", interfaceClass.getName());

        // 分组和版本
        if (group != null) {
            params.put("group", group);
        }
        if (version != null) {
            params.put("version", version);
        }

        // 协议和端口
        String protocolName = (protocolConfig != null) ? protocolConfig.getName() : "dubbo";
        int port = (protocolConfig != null) ? protocolConfig.getPort() : 20880;

        return new URL(protocolName, NetUtils.getLocalHost(), port, interfaceClass.getName(), params);
    }

    /**
     * 构建注册中心 URL
     * 格式：zookeeper://localhost:2181
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

    public T getRef() { return ref; }
    public void setRef(T ref) { this.ref = ref; }

    public ApplicationConfig getApplicationConfig() { return applicationConfig; }
    public void setApplicationConfig(ApplicationConfig applicationConfig) { this.applicationConfig = applicationConfig; }

    public RegistryConfig getRegistryConfig() { return registryConfig; }
    public void setRegistryConfig(RegistryConfig registryConfig) { this.registryConfig = registryConfig; }

    public ProtocolConfig getProtocolConfig() { return protocolConfig; }
    public void setProtocolConfig(ProtocolConfig protocolConfig) { this.protocolConfig = protocolConfig; }

    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
}
