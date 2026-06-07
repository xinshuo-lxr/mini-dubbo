package org.apache.dubbo.spring.boot.beans.factory.annotation;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.spring.boot.annotation.DubboService;
import org.apache.dubbo.spring.boot.annotation.EnableDubbo;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/**
 * 服务暴露后处理器。
 * <p>
 * 阶段 1（BeanDefinitionRegistryPostProcessor）：从 @EnableDubbo 注解读取 scanBasePackages，
 *   扫描 @DubboService 注解的类，注册为 Spring Bean 定义。
 * 阶段 2（ApplicationListener&lt;ContextRefreshedEvent&gt;）：Spring 容器刷新完成后，
 *   为每个 @DubboService Bean 创建 ServiceConfig 并调用 export()。
 */
public class ServiceAnnotationPostProcessor implements BeanDefinitionRegistryPostProcessor, ApplicationListener<ContextRefreshedEvent>, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(ServiceAnnotationPostProcessor.class);

    private Set<String> packagesToScan;
    private ConfigurableListableBeanFactory beanFactory;
    private volatile boolean exported = false;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (applicationContext instanceof org.springframework.context.ConfigurableApplicationContext) {
            this.beanFactory = ((org.springframework.context.ConfigurableApplicationContext) applicationContext).getBeanFactory();
        }
    }

    /**
     * 阶段 1：从 @EnableDubbo 读取扫描路径，扫描 @DubboService 类并注册 Bean 定义。
     */
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        Set<String> packages = resolvePackagesToScan(registry);
        if (packages.isEmpty()) {
            logger.warn("未找到 @EnableDubbo 注解或 scanBasePackages 为空，跳过 @DubboService 扫描");
            return;
        }
        this.packagesToScan = packages;

        BeanNameGenerator beanNameGenerator = AnnotationBeanNameGenerator.INSTANCE;
        for (String pkg : packages) {
            ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry, false);
            scanner.setBeanNameGenerator(beanNameGenerator);
            scanner.addIncludeFilter(new AnnotationTypeFilter(DubboService.class));
            int count = scanner.scan(pkg);
            logger.info("在包 '{}' 下扫描到 {} 个 @DubboService Bean", pkg, count);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // no-op
    }

    /**
     * 阶段 2：Spring 容器刷新完成后，导出 @DubboService 服务。
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (exported || beanFactory == null || packagesToScan == null || packagesToScan.isEmpty()) {
            return;
        }
        exported = true;

        ApplicationConfig applicationConfig = getOptionalBean(beanFactory, ApplicationConfig.class);
        RegistryConfig registryConfig = getOptionalBean(beanFactory, RegistryConfig.class);
        ProtocolConfig protocolConfig = getOptionalBean(beanFactory, ProtocolConfig.class);

        Map<String, Object> serviceBeans = beanFactory.getBeansWithAnnotation(DubboService.class);
        logger.info("发现 {} 个 @DubboService Bean，开始导出服务", serviceBeans.size());

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
     * 从 BeanDefinitionRegistry 中查找标注了 @EnableDubbo 的 Bean 定义，读取 scanBasePackages。
     * 如果为空，则使用 @EnableDubbo 所在类的包作为默认扫描路径。
     */
    private Set<String> resolvePackagesToScan(BeanDefinitionRegistry registry) {
        Set<String> packages = new LinkedHashSet<>();
        for (String beanName : registry.getBeanDefinitionNames()) {
            org.springframework.beans.factory.config.BeanDefinition bd = registry.getBeanDefinition(beanName);
            if (!(bd instanceof AnnotatedBeanDefinition)) {
                continue;
            }
            AnnotationMetadata metadata = ((AnnotatedBeanDefinition) bd).getMetadata();
            if (!metadata.hasAnnotation(EnableDubbo.class.getName())) {
                continue;
            }
            Map<String, Object> attrs = metadata.getAnnotationAttributes(EnableDubbo.class.getName());
            if (attrs == null) {
                continue;
            }
            String[] basePackages = (String[]) attrs.get("scanBasePackages");
            if (basePackages != null) {
                for (String pkg : basePackages) {
                    if (pkg != null && !pkg.isEmpty()) {
                        packages.add(pkg);
                    }
                }
            }
            // 如果 scanBasePackages 为空，使用 @EnableDubbo 所在类的包
            if (packages.isEmpty()) {
                String className = metadata.getClassName();
                String pkg = className.substring(0, className.lastIndexOf('.'));
                packages.add(pkg);
            }
            break;
        }
        return packages;
    }

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

    private <T> T getOptionalBean(ConfigurableListableBeanFactory beanFactory, Class<T> type) {
        Map<String, T> beans = beanFactory.getBeansOfType(type);
        if (beans.isEmpty()) {
            return null;
        }
        return beans.values().iterator().next();
    }
}
