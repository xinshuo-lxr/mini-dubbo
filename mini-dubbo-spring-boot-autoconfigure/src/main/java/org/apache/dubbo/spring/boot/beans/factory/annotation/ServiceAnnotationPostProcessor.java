package org.apache.dubbo.spring.boot.beans.factory.annotation;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.spring.boot.annotation.DubboService;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/**
 * 服务暴露后处理器。
 * <p>
 * 在 Spring 容器启动早期扫描 @DubboService 注解的类，注册为 Spring Bean；
 * 然后在所有 Bean 定义加载完成后，为每个服务实现创建 ServiceConfig 并调用 export()。
 */
public class ServiceAnnotationPostProcessor implements BeanDefinitionRegistryPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ServiceAnnotationPostProcessor.class);

    private final Set<String> packagesToScan;

    public ServiceAnnotationPostProcessor(Set<String> packagesToScan) {
        this.packagesToScan = packagesToScan;
    }

    /**
     * 阶段 1：扫描 @DubboService 注解的类，注册为 Spring Bean 定义。
     */
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        if (packagesToScan == null || packagesToScan.isEmpty()) {
            return;
        }
        BeanNameGenerator beanNameGenerator = AnnotationBeanNameGenerator.INSTANCE;
        for (String pkg : packagesToScan) {
            ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry, false);
            scanner.setBeanNameGenerator(beanNameGenerator);
            scanner.addIncludeFilter(new AnnotationTypeFilter(DubboService.class));
            scanner.scan(pkg);
        }
    }

    /**
     * 阶段 2：从 Spring 容器中获取 @DubboService Bean，创建 ServiceConfig 并 export。
     */
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        ApplicationConfig applicationConfig = getOptionalBean(beanFactory, ApplicationConfig.class);
        RegistryConfig registryConfig = getOptionalBean(beanFactory, RegistryConfig.class);
        ProtocolConfig protocolConfig = getOptionalBean(beanFactory, ProtocolConfig.class);

        Map<String, Object> serviceBeans = beanFactory.getBeansWithAnnotation(DubboService.class);

        for (Object serviceImpl : serviceBeans.values()) {
            Class<?> implClass = serviceImpl.getClass();
            DubboService annotation = implClass.getAnnotation(DubboService.class);
            if (annotation == null) {
                continue;
            }

            Class<?> interfaceClass = resolveInterfaceClass(annotation, implClass);
            ServiceConfig<Object> serviceConfig = new ServiceConfig<>();
            serviceConfig.setInterfaceClass((Class<Object>) interfaceClass);
            serviceConfig.setRef(serviceImpl);
            serviceConfig.setApplicationConfig(applicationConfig);
            serviceConfig.setRegistryConfig(registryConfig);
            serviceConfig.setProtocolConfig(protocolConfig);

            if (!annotation.version().isEmpty()) {
                serviceConfig.setVersion(annotation.version());
            }
            if (!annotation.group().isEmpty()) {
                serviceConfig.setGroup(annotation.group());
            }

            serviceConfig.export();
            logger.info("Dubbo 服务已暴露: {} -> {}", interfaceClass.getName(), implClass.getName());
        }
    }

    /**
     * 确定服务接口类：优先用注解的 interfaceClass，否则从实现的接口中自动检测。
     */
    private Class<?> resolveInterfaceClass(DubboService annotation, Class<?> implClass) {
        if (annotation.interfaceClass() != void.class) {
            return annotation.interfaceClass();
        }
        Class<?>[] interfaces = implClass.getInterfaces();
        if (interfaces.length == 0) {
            throw new BeanCreationException(
                    "@DubboService 标注的类 " + implClass.getName() + " 没有实现任何接口，"
                            + "请通过 interfaceClass 属性指定服务接口");
        }
        for (Class<?> iface : interfaces) {
            String name = iface.getName();
            if (!name.startsWith("java.") && !name.startsWith("javax.")) {
                return iface;
            }
        }
        return interfaces[0];
    }

    /**
     * 从容器获取指定类型的 Bean，不存在时返回 null（不抛异常）。
     */
    private <T> T getOptionalBean(ConfigurableListableBeanFactory beanFactory, Class<T> type) {
        Map<String, T> beans = beanFactory.getBeansOfType(type);
        if (beans.isEmpty()) {
            return null;
        }
        return beans.values().iterator().next();
    }
}
