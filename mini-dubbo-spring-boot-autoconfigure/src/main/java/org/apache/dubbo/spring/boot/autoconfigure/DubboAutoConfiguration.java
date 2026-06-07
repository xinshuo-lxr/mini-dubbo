package org.apache.dubbo.spring.boot.autoconfigure;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.spring.boot.annotation.DubboReference;
import org.apache.dubbo.spring.boot.annotation.DubboService;
import org.apache.dubbo.spring.boot.beans.factory.annotation.ReferenceAnnotationBeanPostProcessor;
import org.apache.dubbo.spring.boot.beans.factory.annotation.ServiceAnnotationPostProcessor;

import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dubbo 自动配置类。
 * <p>
 * 从 application.yml 读取 dubbo.* 配置，创建 ApplicationConfig、RegistryConfig、ProtocolConfig Bean，
 * 并注册 ServiceAnnotationPostProcessor 和 ReferenceAnnotationBeanPostProcessor。
 *
 * @see DubboService
 * @see DubboReference
 * @see ServiceAnnotationPostProcessor
 * @see ReferenceAnnotationBeanPostProcessor
 */
@Configuration
@EnableConfigurationProperties(DubboConfigurationProperties.class)
public class DubboAutoConfiguration {

    /**
     * 从 dubbo.application.* 配置创建 ApplicationConfig Bean。
     */
    @Bean
    public ApplicationConfig applicationConfig(DubboConfigurationProperties properties) {
        return properties.toApplicationConfig();
    }

    /**
     * 从 dubbo.registry.* 配置创建 RegistryConfig Bean。
     */
    @Bean
    public RegistryConfig registryConfig(DubboConfigurationProperties properties) {
        return properties.toRegistryConfig();
    }

    /**
     * 从 dubbo.protocol.* 配置创建 ProtocolConfig Bean。
     */
    @Bean
    public ProtocolConfig protocolConfig(DubboConfigurationProperties properties) {
        return properties.toProtocolConfig();
    }

    /**
     * 创建服务暴露后处理器：扫描 @DubboService 注解的类，自动导出服务。
     * packagesToScan 由 @EnableDubbo 注解的 scanBasePackages 属性提供。
     */
    @Bean
    public static ServiceAnnotationPostProcessor serviceAnnotationPostProcessor(
            @Qualifier("dubbo.packagesToScan") Set<String> packagesToScan) {
        return new ServiceAnnotationPostProcessor(packagesToScan);
    }

    /**
     * 创建服务引用后处理器：扫描 @DubboReference 注解的字段，自动注入代理对象。
     */
    @Bean
    public static ReferenceAnnotationBeanPostProcessor referenceAnnotationBeanPostProcessor() {
        return new ReferenceAnnotationBeanPostProcessor();
    }
}
