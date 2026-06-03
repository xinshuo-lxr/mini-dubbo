package org.apache.dubbo.common.extension;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自动激活注解。用于 Filter 等需要根据条件自动装配的场景。
 *
 * group: 只在 consumer 侧或 provider 侧激活
 * order: 排序，越小越先执行
 *
 * 对应 Dubbo 源码：org.apache.dubbo.common.extension.Activate
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Activate {
    String[] group() default {};
    int order() default 0;
}
