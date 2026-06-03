package org.apache.dubbo.common.extension;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自适应扩展注解。
 *
 * 两种用法：
 * 1. 标注在类上 → 这个类就是自适应实现，直接用 -- 表示这个类自己实现了获取对应的实现
 * 2. 标注在方法上 → 框架动态生成代理类，运行时根据 URL 参数选择实现 主要是这一种
 *
 * 对应 Dubbo 源码：org.apache.dubbo.common.extension.Adaptive
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Adaptive {
    String[] value() default {};
}
