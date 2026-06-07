package org.apache.dubbo.spring.boot.beans.factory.annotation;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.stereotype.Component;

/**
 * 服务引用后处理器
 * <p>
 * 扫描 @DubboReference 注解的字段，在 Bean 属性填充阶段注入代理对象。
 */
@Component
public class ReferenceAnnotationBeanPostProcessor implements InstantiationAwareBeanPostProcessor {

    private DefaultListableBeanFactory beanFactory;

    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
        return null;
    }

    @Override
    public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
        return true;
    }

    @Override
    public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) throws BeansException {
        // TODO: 实现 — 扫描 @DubboReference 字段，注入代理对象
        return pvs;
    }
}
