package org.apache.dubbo.spring.boot.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个字段，用于注入 Dubbo 服务引用（代理对象）。
 *
 * <pre>
 * &#64;Component
 * public class ConsumerController {
 *     &#64;DubboReference
 *     private DemoService demoService;
 * }
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface DubboReference {

    /**
     * 服务接口类，默认 void.class（自动从字段类型中检测）。
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

    /**
     * 服务调用超时时间（毫秒），默认 -1（使用全局配置）。
     */
    int timeout() default -1;

    /**
     * failover 集群重试次数，默认 -1（使用全局配置）。
     */
    int retries() default -1;
}
