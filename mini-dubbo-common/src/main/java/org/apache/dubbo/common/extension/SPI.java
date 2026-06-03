package org.apache.dubbo.common.extension;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个接口为 Dubbo SPI 扩展点。
 *
 * value() 指定默认扩展名。例如 @SPI("dubbo") 表示不指定名字时用 "dubbo" 对应的实现。
 *
 * 对应 Dubbo 源码：org.apache.dubbo.common.extension.SPI
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SPI {
    String value() default "";
}
