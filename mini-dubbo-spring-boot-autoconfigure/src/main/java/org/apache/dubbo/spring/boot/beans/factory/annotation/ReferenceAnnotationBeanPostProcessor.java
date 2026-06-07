package org.apache.dubbo.spring.boot.beans.factory.annotation;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.spring.boot.annotation.DubboReference;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * 服务引用后处理器。
 * <p>
 * 扫描 @DubboReference 注解的字段，在 Bean 属性填充阶段注入代理对象。
 * 对相同接口+版本+分组的引用复用同一个 ReferenceConfig，避免重复创建连接。
 * <p>
 * 由 {@link org.apache.dubbo.spring.boot.autoconfigure.DubboAutoConfiguration} 注册为 Bean。
 */
public class ReferenceAnnotationBeanPostProcessor implements InstantiationAwareBeanPostProcessor, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(ReferenceAnnotationBeanPostProcessor.class);

    /** 缓存已创建的代理对象，key = interfaceClass#version#group */
    private final Map<String, Object> proxyCache = new ConcurrentHashMap<>();

    private DefaultListableBeanFactory beanFactory;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (applicationContext.getAutowireCapableBeanFactory() instanceof DefaultListableBeanFactory) {
            this.beanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
        }
    }

    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
        return null;
    }

    @Override
    public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
        return true;
    }

    /**
     * 扫描 Bean 中所有 @DubboReference 标注的字段，注入代理对象。
     */
    @Override
    public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        // 跳过 JDK 代理类，获取目标类
        if (beanClass.getName().contains("$$")) {
            beanClass = beanClass.getSuperclass();
        }
        injectDubboReferences(bean, beanClass);
        return pvs;
    }

    /**
     * 递归扫描类层级中的 @DubboReference 字段并注入。
     */
    private void injectDubboReferences(Object bean, Class<?> clazz) {
        if (clazz == null || clazz == Object.class) {
            return;
        }
        for (Field field : clazz.getDeclaredFields()) {
            DubboReference annotation = field.getAnnotation(DubboReference.class);
            if (annotation == null) {
                continue;
            }
            Object proxy = getOrCreateProxy(annotation, field);
            field.setAccessible(true);
            try {
                field.set(bean, proxy);
                logger.info("Dubbo 引用已注入: {}.{}", bean.getClass().getSimpleName(), field.getName());
            } catch (IllegalAccessException e) {
                throw new BeansException("注入 @DubboReference 字段失败: " + field.getName(), e) {};
            }
        }
        // 递归处理父类
        injectDubboReferences(bean, clazz.getSuperclass());
    }

    /**
     * 获取或创建代理对象（带缓存）。
     */
    private Object getOrCreateProxy(DubboReference annotation, Field field) {
        Class<?> interfaceClass = resolveInterfaceClass(annotation, field);
        String cacheKey = buildCacheKey(interfaceClass, annotation.version(), annotation.group());

        return proxyCache.computeIfAbsent(cacheKey, key -> createProxy(interfaceClass, annotation));
    }

    /**
     * 创建 ReferenceConfig 并获取代理对象。
     */
    @SuppressWarnings("unchecked")
    private <T> Object createProxy(Class<T> interfaceClass, DubboReference annotation) {
        ApplicationConfig applicationConfig = beanFactory.getBeanProvider(ApplicationConfig.class).getIfAvailable();
        RegistryConfig registryConfig = beanFactory.getBeanProvider(RegistryConfig.class).getIfAvailable();

        ReferenceConfig<T> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterfaceClass(interfaceClass);
        referenceConfig.setApplicationConfig(applicationConfig);
        referenceConfig.setRegistryConfig(registryConfig);

        if (!annotation.version().isEmpty()) {
            referenceConfig.setVersion(annotation.version());
        }
        if (!annotation.group().isEmpty()) {
            referenceConfig.setGroup(annotation.group());
        }
        if (annotation.timeout() >= 0) {
            referenceConfig.setTimeout(annotation.timeout());
        }
        if (annotation.retries() >= 0) {
            referenceConfig.setRetries(annotation.retries());
        }

        T proxy = referenceConfig.get();
        logger.info("Dubbo 引用代理已创建: {}", interfaceClass.getName());
        return proxy;
    }

    /**
     * 确定服务接口类：优先用注解的 interfaceClass，否则用字段类型。
     */
    private Class<?> resolveInterfaceClass(DubboReference annotation, Field field) {
        if (annotation.interfaceClass() != void.class) {
            return annotation.interfaceClass();
        }
        return field.getType();
    }

    /**
     * 构建缓存 key：interfaceClass#version#group。
     */
    private String buildCacheKey(Class<?> interfaceClass, String version, String group) {
        return interfaceClass.getName() + "#" + version + "#" + group;
    }
}
