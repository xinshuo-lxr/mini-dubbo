package org.apache.dubbo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 启动入口 — 链式 API，组装所有配置，触发 export/refer。
 *
 * 用法：
 *
 * // Provider 端
 * Bootstrap.provider()
 *     .application(new ApplicationConfig("demo-provider"))
 *     .registry(new RegistryConfig("localhost:2181"))
 *     .protocol(new ProtocolConfig("dubbo", 20880))
 *     .service(DemoService.class, new DemoServiceImpl())
 *     .start();
 *
 * // Consumer 端
 * DemoService demoService = Bootstrap.consumer()
 *     .application(new ApplicationConfig("demo-consumer"))
 *     .registry(new RegistryConfig("localhost:2181"))
 *     .reference(DemoService.class)
 *     .start();
 *
 * 对应 Dubbo 源码：DubboBootstrap（简化版）
 */
public class Bootstrap {

    private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    private ApplicationConfig applicationConfig;
    private RegistryConfig registryConfig;
    private ProtocolConfig protocolConfig;

    /** Provider 端：待暴露的服务列表 */
    private final List<ServiceConfig<?>> services = new ArrayList<>();

    /** Consumer 端：待引用的服务列表 */
    private final List<ReferenceConfig<?>> references = new ArrayList<>();

    private Bootstrap() {}

    // ==================== 工厂方法 ====================

    /**
     * 创建 Provider 端 Bootstrap
     */
    public static Bootstrap provider() {
        return new Bootstrap();
    }

    /**
     * 创建 Consumer 端 Bootstrap
     */
    public static Bootstrap consumer() {
        return new Bootstrap();
    }

    // ==================== 链式配置 ====================

    public Bootstrap application(ApplicationConfig config) {
        this.applicationConfig = config;
        return this;
    }

    public Bootstrap registry(RegistryConfig config) {
        this.registryConfig = config;
        return this;
    }

    public Bootstrap protocol(ProtocolConfig config) {
        this.protocolConfig = config;
        return this;
    }

    /**
     * 添加待暴露的服务（Provider 端）
     */
    public <T> Bootstrap service(Class<T> interfaceClass, T impl) {
        ServiceConfig<T> config = new ServiceConfig<>(interfaceClass, impl);
        config.setApplicationConfig(applicationConfig);
        config.setRegistryConfig(registryConfig);
        config.setProtocolConfig(protocolConfig);
        services.add(config);
        return this;
    }

    /**
     * 添加待引用的服务（Consumer 端），返回 ReferenceConfig 以便获取代理
     */
    public <T> ReferenceConfig<T> reference(Class<T> interfaceClass) {
        ReferenceConfig<T> config = new ReferenceConfig<>(interfaceClass);
        config.setApplicationConfig(applicationConfig);
        config.setRegistryConfig(registryConfig);
        references.add(config);
        return config;
    }

    // ==================== 启动 ====================

    /**
     * 启动 — Provider 端暴露所有服务
     *
     * 对应 Dubbo 源码：DubboBootstrap.exportServices()
     */
    public Bootstrap start() {
        logger.info("Starting mini-dubbo...");
        logger.info("Application: " + (applicationConfig != null ? applicationConfig.getName() : "N/A"));
        logger.info("Registry: " + (registryConfig != null ? registryConfig.getAddress() : "N/A"));

        for (ServiceConfig<?> service : services) {
            service.export();
        }

        logger.info("mini-dubbo started. Exported " + services.size() + " service(s).");
        return this;
    }

    /**
     * 关闭 — 取消所有暴露的服务
     */
    public void shutdown() {
        logger.info("Shutting down mini-dubbo...");
        for (ServiceConfig<?> service : services) {
            service.unexport();
        }
        logger.info("mini-dubbo shutdown complete.");
    }
}
