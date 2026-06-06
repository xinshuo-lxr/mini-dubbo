package org.apache.dubbo.config;

/**
 * 应用配置 — 应用名、日志级别等。
 *
 * 对应 Dubbo 源码：org.apache.dubbo.config.ApplicationConfig
 */
public class ApplicationConfig {

    private String name;
    private String logger;

    public ApplicationConfig() {}

    public ApplicationConfig(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLogger() { return logger; }
    public void setLogger(String logger) { this.logger = logger; }
}
