package org.apache.dubbo.spring.boot.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 启用 Dubbo 注解扫描和自动配置。
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
     * 扫描 @DubboService 和 @DubboReference 注解的基础包路径。
     * 为空时扫描注解所在类的包及其子包。
     */
    String[] scanBasePackages() default {};
}
