package org.apache.dubbo.common.extension;

import org.apache.dubbo.common.URL;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SPI 扩展加载器 — Dubbo SPI 的核心引擎。
 *
 * 每个 SPI 接口对应一个 ExtensionLoader 实例。
 * 负责从 META-INF/dubbo/internal/ 加载配置，按名获取扩展，生成自适应代理。
 *
 * 对应 Dubbo 源码：org.apache.dubbo.common.extension.ExtensionLoader
 * 简化点：去掉 ScopeModel 隔离、缓存优化、Wrapper 排序等
 */
public class ExtensionLoader<T> {

    private static final String DUBBO_DIRECTORY = "META-INF/dubbo/internal/";

    /** ExtensionLoader 缓存：每个 SPI 接口一个 */
    private static final ConcurrentMap<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<>();

    /** 已创建的扩展实例缓存 */
    private static final ConcurrentMap<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<>();

    /** 用于并发控制 */
    private final ConcurrentMap<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<>();
    /** 当前 SPI 接口 */
    private final Class<T> type;

    /** name → Class 映射（从配置文件加载） */
    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<>();

    /** @Adaptive 标注在类上的情况 */
    private volatile Class<?> cachedAdaptiveClass;

    /** Wrapper 类（有单参数构造器，参数类型是 SPI 接口） */
    private final List<Class<?>> cachedWrapperClasses = new ArrayList<>();

    /** @Activate 注解的类缓存 */
    private final Map<String, Activate> cachedActivates = new ConcurrentHashMap<>();

    /** @SPI 默认扩展名 */
    private String cachedDefaultName;

    /** 已创建的自适应实例 */
    private final Holder<Object> cachedAdaptiveInstance = new Holder<>();

    private ExtensionLoader(Class<T> type) {
        this.type = type;
        // 读取 @SPI 注解的默认值
        SPI spi = type.getAnnotation(SPI.class);
        if (spi != null) {
            cachedDefaultName = spi.value();
        }
    }

    /**
     * 获取指定类型的 ExtensionLoader（静态工厂方法）
     */
    @SuppressWarnings("unchecked")
    public static <S> ExtensionLoader<S> getExtensionLoader(Class<S> type) {
        if (type == null) {
            throw new IllegalArgumentException("Extension type == null");
        }
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension type (" + type + ") is not an interface!");
        }
        if (type.getAnnotation(SPI.class) == null) {
            throw new IllegalArgumentException("Extension type (" + type + ") without @SPI Annotation!");
        }
        ExtensionLoader<S> loader = (ExtensionLoader<S>) EXTENSION_LOADERS.get(type);
        if (loader == null) {
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<>(type));
            loader = (ExtensionLoader<S>) EXTENSION_LOADERS.get(type);
        }
        return loader;
    }

    /**
     * 按名获取扩展实例（经过 Wrapper 包装）
     * "true" → 返回默认扩展
     */
    public T getExtension(String name) {
        if ("true".equals(name)) {
            return getDefaultExtension();
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Extension name == null");
        }
        // 将一个name的获取锁定
        Holder<Object> holder = cachedInstances.computeIfAbsent(name, k -> new Holder<>());
        Object instance = holder.get();
        if (instance == null) {
            synchronized (holder) {
                instance = holder.get();
                if (instance == null) {
                    instance = createExtension(name);
                    holder.set(instance);
                }
            }
        }
        return type.cast(instance);
    }

    /**
     * 获取默认扩展（@SPI 注解的 value 指定的）
     */
    public T getDefaultExtension() {
        if (cachedDefaultName == null || cachedDefaultName.isEmpty()) {
            return null;
        }
        return getExtension(cachedDefaultName);
    }

    /**
     * 获取自适应扩展
     *
     * 如果有类标注了 @Adaptive → 直接返回该类实例
     * 否则 → 动态生成代理类
     */
    public T getAdaptiveExtension() {
        Object instance = cachedAdaptiveInstance.get();
        if (instance == null) {
            synchronized (cachedAdaptiveInstance) {
                instance = cachedAdaptiveInstance.get();
                if (instance == null) {
                    instance = createAdaptiveExtension();
                    cachedAdaptiveInstance.set(instance);
                }
            }
        }
        return type.cast(instance);
    }

    /**
     * 获取所有 @Activate 注解的扩展（按 group 过滤，按 order 排序）
     */
    public List<T> getActivateExtension(URL url, String key, String group) {
        Map<String, Class<?>> extensionClasses = getExtensionClasses();
        List<T> activates = new ArrayList<>();
        for (Map.Entry<String, Class<?>> entry : extensionClasses.entrySet()) {
            String name = entry.getKey();
            Class<?> clazz = entry.getValue();
            Activate activate = cachedActivates.get(name);
            if (activate != null) {
                if (isMatchGroup(group, activate.group())) {
                    activates.add(getExtension(name));
                }
            }
        }
        // 按 order 排序
        activates.sort((a, b) -> {
            Activate aa = a.getClass().getAnnotation(Activate.class);
            Activate bb = b.getClass().getAnnotation(Activate.class);
            int orderA = aa != null ? aa.order() : 0;
            int orderB = bb != null ? bb.order() : 0;
            return Integer.compare(orderA, orderB);
        });
        return activates;
    }

    /**
     * 获取所有已加载的扩展名
     */
    public Set<String> getSupportedExtensions() {
        Map<String, Class<?>> classes = getExtensionClasses();
        return Collections.unmodifiableSet(classes.keySet());
    }

    // ==================== 内部实现 ====================

    @SuppressWarnings("unchecked")
    private T createExtension(String name) {
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null) {
            throw new IllegalStateException("No such extension \"" + name + "\" for " + type.getName());
        }
        T instance = (T) EXTENSION_INSTANCES.get(clazz);
        if (instance == null) {
            try {
                EXTENSION_INSTANCES.putIfAbsent(clazz, clazz.newInstance());
                instance = (T) EXTENSION_INSTANCES.get(clazz);
            } catch (Exception e) {
                throw new IllegalStateException("Extension instance(name: " + name + ", class: " + clazz + ") could not be instantiated: " + e.getMessage(), e);
            }
        }
        // 应用 Wrapper 包装
        for (Class<?> wrapperClass : cachedWrapperClasses) {
            try {
                Constructor<?> constructor = wrapperClass.getConstructor(type);
                instance = type.cast(constructor.newInstance(instance));
            } catch (Exception e) {
                throw new IllegalStateException("Wrapper instance error: " + e.getMessage(), e);
            }
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    private T createAdaptiveExtension() {
        try {
            Class<?> clazz = getAdaptiveExtensionClass();
            if (clazz != null && clazz.getAnnotation(Adaptive.class) != null) {
                // 有 @Adaptive 标注在类上 → 直接实例化该类
                return type.cast(clazz.newInstance());
            }
            // 没有 @Adaptive 类 → 动态生成代理（从方法上的 @Adaptive 读 URL 参数）
            return type.cast(Proxy.newProxyInstance(
                    type.getClassLoader(),
                    new Class[]{type},
                    new AdaptiveInvocationHandler(type, cachedDefaultName)
            ));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create adaptive extension: " + e.getMessage(), e);
        }
    }

    private Class<?> getAdaptiveExtensionClass() {
        getExtensionClasses();
        if (cachedAdaptiveClass != null) {
            return cachedAdaptiveClass;
        }
        // 没有 @Adaptive 标注的类，返回 null，由调用方生成代理
        return null;
    }

    /**
     * 加载所有扩展类（从配置文件）
     */
    private Map<String, Class<?>> getExtensionClasses() {
        Map<String, Class<?>> classes = cachedClasses.get();
        if (classes == null) {
            synchronized (cachedClasses) {
                classes = cachedClasses.get();
                if (classes == null) {
                    classes = loadExtensionClasses();
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }

    private Map<String, Class<?>> loadExtensionClasses() {
        Map<String, Class<?>> extensionClasses = new HashMap<>();
        String fileName = DUBBO_DIRECTORY + type.getName();
        try {
            ClassLoader classLoader = ExtensionLoader.class.getClassLoader();
            java.net.URL url = classLoader.getResource(fileName);
            if (url != null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }
                        // 格式：name=className
                        int eq = line.indexOf('=');
                        if (eq > 0) {
                            String name = line.substring(0, eq).trim();
                            String className = line.substring(eq + 1).trim();
                            Class<?> clazz = Class.forName(className, true, classLoader);
                            cacheClass(extensionClasses, name, clazz);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load extension class for " + type.getName() + ": " + e.getMessage(), e);
        }
        return extensionClasses;
    }

    /**
     * 分类缓存：@Adaptive 在类上 → cachedAdaptiveClass；是 Wrapper → cachedWrapperClasses；否则按名缓存
     */
    private void cacheClass(Map<String, Class<?>> extensionClasses, String name, Class<?> clazz) {
        if (clazz.isAnnotationPresent(Adaptive.class)) {
            cachedAdaptiveClass = clazz;
        } else if (isWrapperClass(clazz)) {
            cachedWrapperClasses.add(clazz);
        } else {
            extensionClasses.put(name, clazz);
            // 缓存 @Activate 注解
            if (clazz.isAnnotationPresent(Activate.class)) {
                cachedActivates.put(name, clazz.getAnnotation(Activate.class));
            }
        }
    }

    /**
     * 判断是否是 Wrapper 类：有单参数构造器，参数类型是 SPI 接口
     */
    private boolean isWrapperClass(Class<?> clazz) {
        try {
            Constructor<?>[] constructors = clazz.getConstructors();
            for (Constructor<?> constructor : constructors) {
                if (constructor.getParameterTypes().length == 1
                        && constructor.getParameterTypes()[0] == type) {
                    return true;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

    private boolean isMatchGroup(String group, String[] groups) {
        if (group == null || group.isEmpty()) {
            return true;
        }
        if (groups == null || groups.length == 0) {
            return true;
        }
        for (String g : groups) {
            if (group.equals(g)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "ExtensionLoader[" + type.getName() + "]";
    }

    /**
     * 简单的 Holder，用于 double-checked locking
     */
    private static class Holder<T> {
        private volatile T value;
        public T get() { return value; }
        public void set(T value) { this.value = value; }
    }

    /**
     * @Adaptive 代理的 InvocationHandler
     * 当 @Adaptive 标注在方法上时，动态代理读取 URL 参数来选择扩展
     */
    private static class AdaptiveInvocationHandler implements java.lang.reflect.InvocationHandler {
        private final Class<?> type;
        private final String defaultName;

        public AdaptiveInvocationHandler(Class<?> type, String defaultName) {
            this.type = type;
            this.defaultName = defaultName;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // Object 方法直接代理
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }

            // 从参数中找到 URL
            URL url = findUrl(args);
            if (url == null) {
                throw new IllegalStateException("Failed to find URL in args for method " + method.getName());
            }

            // 确定扩展名：先从 @Adaptive.value() 取 key，再从 URL 取参数，最后用默认值
            Adaptive adaptive = method.getAnnotation(Adaptive.class);
            String extName = null;
            if (adaptive != null && adaptive.value().length > 0) {
                for (String key : adaptive.value()) {
                    extName = url.getParameter(key);
                    if (extName != null && !extName.isEmpty()) {
                        break;
                    }
                }
            }
            if (extName == null || extName.isEmpty()) {
                // 从接口名推导 key（如 Protocol → protocol）
                extName = url.getProtocol();
            }
            if (extName == null || extName.isEmpty()) {
                extName = defaultName;
            }

            // 获取真正的扩展实例
            Object extension = ExtensionLoader.getExtensionLoader(type).getExtension(extName);
            return method.invoke(extension, args);
        }

        private URL findUrl(Object[] args) {
            if (args == null) return null;
            for (Object arg : args) {
                if (arg instanceof org.apache.dubbo.common.URL) {
                    return (org.apache.dubbo.common.URL) arg;
                }
                // 尝试通过 getUrl() 方法获取
                try {
                    Method getUrl = arg.getClass().getMethod("getUrl");
                    Object result = getUrl.invoke(arg);
                    if (result instanceof org.apache.dubbo.common.URL) {
                        return (org.apache.dubbo.common.URL) result;
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
            return null;
        }
    }
}
