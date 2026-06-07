package org.apache.dubbo.spring.boot.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个类为 Dubbo 服务提供者。
 * <p>
 * 由 {@link org.apache.dubbo.spring.boot.beans.factory.annotation.ServiceAnnotationPostProcessor}
 * 扫描并注册为 Spring Bean，不需要 @Component。
 *
 * <pre>
 * &#64;DubboService
 * public class DemoServiceImpl implements DemoService { ... }
 *
 * &#64;DubboService(interfaceClass = DemoService.class, version = "1.0.0", group = "demo")
 * public class DemoServiceImpl implements DemoService { ... }
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
public @interface DubboService {

    /**
     * 服务接口类，默认 void.class（自动从实现的接口中检测）。
     */
    Class<?> interfaceClass() default void.class;

    /**
     * 服务版本号，默认空字符串。
     */
    String version() default "";

    /**
     * 服务分组，默认空字符串。
     */
    String group() default "";
}
