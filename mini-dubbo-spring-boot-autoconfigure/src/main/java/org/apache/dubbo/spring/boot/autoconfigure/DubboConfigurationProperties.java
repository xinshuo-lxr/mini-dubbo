package org.apache.dubbo.spring.boot.autoconfigure;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 绑定 application.yml 中 dubbo.* 配置到对应的 Config POJO。
 *
 * <pre>
 * dubbo:
 *   application:
 *     name: demo-provider
 *   registry:
 *     address: zookeeper://localhost:2181
 *   protocol:
 *     name: dubbo
 *     port: 20880
 * </pre>
 */
@ConfigurationProperties(prefix = "dubbo")
public class DubboConfigurationProperties {

    private Application application = new Application();
    private Registry registry = new Registry();
    private Protocol protocol = new Protocol();

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public Registry getRegistry() {
        return registry;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    /**
     * 将绑定的属性转换为 mini-dubbo 的 ApplicationConfig。
     */
    public ApplicationConfig toApplicationConfig() {
        ApplicationConfig config = new ApplicationConfig();
        config.setName(application.getName());
        return config;
    }

    /**
     * 将绑定的属性转换为 mini-dubbo 的 RegistryConfig。
     */
    public RegistryConfig toRegistryConfig() {
        RegistryConfig config = new RegistryConfig();
        if (registry.getProtocol() != null) {
            config.setProtocol(registry.getProtocol());
        }
        if (registry.getAddress() != null) {
            config.setAddress(registry.getAddress());
        }
        return config;
    }

    /**
     * 将绑定的属性转换为 mini-dubbo 的 ProtocolConfig。
     */
    public ProtocolConfig toProtocolConfig() {
        ProtocolConfig config = new ProtocolConfig();
        if (protocol.getName() != null) {
            config.setName(protocol.getName());
        }
        if (protocol.getPort() > 0) {
            config.setPort(protocol.getPort());
        }
        return config;
    }

    /**
     * dubbo.application.* 配置
     */
    public static class Application {
        /** 应用名称 */
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * dubbo.registry.* 配置
     */
    public static class Registry {
        /** 注册中心协议，默认 zookeeper */
        private String protocol;
        /** 注册中心地址，默认 localhost:2181 */
        private String address;

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }
    }

    /**
     * dubbo.protocol.* 配置
     */
    public static class Protocol {
        /** 协议名称，默认 dubbo */
        private String name;
        /** 服务端口，默认 20880 */
        private int port;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }
}
