package org.apache.dubbo.config;

/**
 * 协议配置 — 协议名、端口等。
 *
 * 对应 Dubbo 源码：org.apache.dubbo.config.ProtocolConfig
 */
public class ProtocolConfig {

    /** 协议名，默认 dubbo */
    private String name = "dubbo";

    /** 服务端口，默认 20880 */
    private int port = 20880;

    public ProtocolConfig() {}

    public ProtocolConfig(String name, int port) {
        this.name = name;
        this.port = port;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
}
