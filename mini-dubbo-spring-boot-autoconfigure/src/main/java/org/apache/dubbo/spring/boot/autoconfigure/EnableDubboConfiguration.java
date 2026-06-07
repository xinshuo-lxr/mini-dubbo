package org.apache.dubbo.spring.boot.autoconfigure;

import org.apache.dubbo.spring.boot.annotation.EnableDubbo;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

/**
 * 由 @EnableDubbo 注解导入的配置类。
 * <p>
 * 读取 @EnableDubbo 的 scanBasePackages 属性
 * 供 {@link DubboAutoConfiguration} 中的 ServiceAnnotationPostProcessor 使用。
 */
@Configuration
public class EnableDubboConfiguration implements ImportAware {

    private static final String BEAN_NAME = "dubbo.packagesToScan";

    private AnnotationAttributes annotationAttributes;

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        Map<String, Object> annotationMap = importMetadata.getAnnotationAttributes(EnableDubbo.class.getName());
        if (annotationMap != null) {
            this.annotationAttributes = AnnotationAttributes.fromMap(annotationMap);
        }
    }

    /**
     * 注册要扫描的包路径集合。ServiceAnnotationPostProcessor 通过 @Qualifier(BEAN_NAME) 注入。
     */
    @Bean(name = BEAN_NAME)
    public Set<String> packagesToScan() {
        Set<String> packages = new LinkedHashSet<>();
        if (annotationAttributes != null) {
            String[] basePackages = annotationAttributes.getStringArray("scanBasePackages");
            for (String pkg : basePackages) {
                if (pkg != null && !pkg.isEmpty()) {
                    packages.add(pkg);
                }
            }
        }
        return packages;
    }
}
