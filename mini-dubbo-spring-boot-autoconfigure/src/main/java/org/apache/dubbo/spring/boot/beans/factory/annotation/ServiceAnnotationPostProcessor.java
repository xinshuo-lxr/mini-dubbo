package org.apache.dubbo.spring.boot.beans.factory.annotation;

import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

/**
 * 服务暴露后处理器。
 * <p>
 * 扫描 @DubboService 注解的类，为其创建 ServiceConfig 并调用 export()。
 */
public class ServiceAnnotationPostProcessor implements BeanDefinitionRegistryPostProcessor {

    private final Set<String> packagesToScan;

    public ServiceAnnotationPostProcessor(Set<String> packagesToScan) {
        this.packagesToScan = packagesToScan;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        // TODO: 实现 — 扫描 @DubboService，注册 ServiceConfig Bean
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // no-op
    }
}
