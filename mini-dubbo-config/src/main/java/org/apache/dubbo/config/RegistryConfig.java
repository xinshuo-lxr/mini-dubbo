package org.apache.dubbo.config;

/**
 * 注册中心配置 — ZK 地址、协议等。
 *
 * 对应 Dubbo 源码：org.apache.dubbo.config.RegistryConfig
 */
public class RegistryConfig {

    /** 注册中心协议，默认 zookeeper */
    private String protocol = "zookeeper";

    /** 注册中心地址，默认 localhost:2181 */
    private String address = "localhost:2181";

    public RegistryConfig() {}

    public RegistryConfig(String address) {
        this.address = address;
    }

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
