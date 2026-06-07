package org.apache.dubbo.spring.boot.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 启用 Dubbo 注解扫描和自动配置。
 * <p>
 * {@link org.apache.dubbo.spring.boot.beans.factory.annotation.ServiceAnnotationPostProcessor}
 * 会从标注了本注解的类上读取 scanBasePackages 属性，扫描 @DubboService 标注的类并注册为 Bean。
 *
 * <pre>
 * &#64;SpringBootApplication
 * &#64;EnableDubbo(scanBasePackages = "org.apache.dubbo.demo")
 * public class ProviderApplication { ... }
 * </pre>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface EnableDubbo {

    /**
     * 扫描 @DubboService 注解的基础包路径。
     * 为空时扫描注解所在类的包及其子包。
     */
    String[] scanBasePackages() default {};
}
