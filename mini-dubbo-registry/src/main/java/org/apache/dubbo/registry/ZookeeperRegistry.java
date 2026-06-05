package org.apache.dubbo.registry;

import org.apache.dubbo.common.URL;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ZooKeeper 注册中心实现 — 使用 Curator 操作 ZK。
 *
 * ZK 节点结构：
 * /dubbo
 *   └── {interfaceName}
 *         ├── providers
 *         │     ├── dubbo://192.168.1.100:20880/DemoService?...  (临时节点)
 *         │     └── dubbo://192.168.1.101:20880/DemoService?...  (临时节点)
 *         └── consumers
 *               └── consumer://192.168.1.200/DemoService?...     (临时节点)
 *
 * Provider 启动 → 在 /providers 下创建临时节点
 * Provider 下线 → ZK 临时节点自动删除 → Consumer 收到通知
 * Consumer 启动 → 监听 /providers 目录变化
 *
 * 对应 Dubbo 源码：org.apache.dubbo.registry.zookeeper.ZookeeperRegistry
 */
public class ZookeeperRegistry implements Registry {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperRegistry.class);

    private final CuratorFramework client;
    private final URL registryUrl;

    /** 订阅缓存：path → TreeCache */
    private final Map<String, TreeCache> treeCacheMap = new ConcurrentHashMap<>();

    /** 已注册的节点路径（用于 unregister） */
    private final Set<String> registeredPaths = ConcurrentHashMap.newKeySet();

    public ZookeeperRegistry(URL url) {
        this.registryUrl = url;
        this.client = createClient(url);
    }

    /**
     * 创建 Curator 客户端
     */
    private CuratorFramework createClient(URL url) {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(url.getHost() + ":" + url.getPort())
                .retryPolicy(retryPolicy)
                .build();
        client.start();
        logger.info("ZooKeeper client connected to " + url.getHost() + ":" + url.getPort());
        return client;
    }

    /**
     * Provider 端：注册服务地址到 ZK
     *
     * 创建临时节点：/dubbo/{interfaceName}/providers/{encodedUrl}
     */
    @Override
    public void register(URL url) {
        String path = toProviderPath(url);
        try {
            if (client.checkExists().forPath(path) == null) {
                // 确保父目录存在
                ensurePath(path);
                // 创建临时节点
                client.create().creatingParentsIfNeeded()
                        .withMode(CreateMode.EPHEMERAL)
                        .forPath(path, url.toFullString().getBytes(StandardCharsets.UTF_8));
                registeredPaths.add(path);
                logger.info("Registered service: " + url.getServiceKey() + " -> " + path);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to register: " + e.getMessage(), e);
        }
    }

    /**
     * Provider 端：取消注册
     */
    @Override
    public void unregister(URL url) {
        String path = toProviderPath(url);
        try {
            if (client.checkExists().forPath(path) != null) {
                client.delete().forPath(path);
                registeredPaths.remove(path);
                logger.info("Unregistered service: " + url.getServiceKey());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to unregister: " + e.getMessage(), e);
        }
    }

    /**
     * Consumer 端：订阅服务变化
     *
     * 监听 /dubbo/{interfaceName}/providers 目录，
     * 当 Provider 上下线时，通过 listener.notify() 通知。
     */
    @Override
    public void subscribe(URL url, NotifyListener listener) {
        String serviceInterface = url.getServiceInterface();
        String providersPath = "/dubbo/" + serviceInterface + "/providers";

        try {
            // 确保路径存在
            if (client.checkExists().forPath(providersPath) == null) {
                client.create().creatingParentsIfNeeded().forPath(providersPath);
            }

            // 使用 TreeCache 监听目录变化
            TreeCache treeCache = new TreeCache(client, providersPath);
            treeCache.getListenable().addListener((curatorFramework, event) -> {
                if (event.getType() == TreeCacheEvent.Type.NODE_ADDED
                        || event.getType() == TreeCacheEvent.Type.NODE_REMOVED
                        || event.getType() == TreeCacheEvent.Type.NODE_UPDATED) {
                    // 收到变化通知，重新获取最新的 Provider 列表
                    List<URL> urls = lookup(url);
                    listener.notify(urls);
                    logger.info("Notified listener for " + serviceInterface + ", providers: " + urls.size());
                }
            });
            treeCache.start();
            treeCacheMap.put(providersPath, treeCache);

            // 立即获取当前 Provider 列表并通知
            List<URL> urls = lookup(url);
            listener.notify(urls);
            logger.info("Subscribed to " + providersPath + ", current providers: " + urls.size());

        } catch (Exception e) {
            throw new RuntimeException("Failed to subscribe: " + e.getMessage(), e);
        }
    }

    /**
     * 取消订阅
     */
    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        String serviceInterface = url.getServiceInterface();
        String providersPath = "/dubbo/" + serviceInterface + "/providers";
        TreeCache treeCache = treeCacheMap.remove(providersPath);
        if (treeCache != null) {
            treeCache.close();
        }
    }

    /**
     * 获取当前已注册的所有 Provider URL
     *
     * 读取 /dubbo/{interfaceName}/providers 下的所有子节点
     */
    @Override
    public List<URL> lookup(URL url) {
        String serviceInterface = url.getServiceInterface();
        String providersPath = "/dubbo/" + serviceInterface + "/providers";
        List<URL> urls = new ArrayList<>();

        try {
            if (client.checkExists().forPath(providersPath) != null) {
                List<String> children = client.getChildren().forPath(providersPath);
                for (String child : children) {
                    String decoded = URLDecoder.decode(child, "UTF-8");
                    urls.add(URL.valueOf(decoded));
                }
            }
        } catch (Exception e) {
            logger.error("Failed to lookup: " + e.getMessage(), e);
        }

        return urls;
    }

    /**
     * 构建 Provider 的 ZK 节点路径
     * /dubbo/{interfaceName}/providers/{encodedUrl}
     */
    private String toProviderPath(URL url) {
        String serviceInterface = url.getServiceInterface();
        String encodedUrl = encodeUrl(url.toFullString());
        return "/dubbo/" + serviceInterface + "/providers/" + encodedUrl;
    }

    /**
     * 确保路径的父目录存在
     */
    private void ensurePath(String path) throws Exception {
        String parent = path.substring(0, path.lastIndexOf('/'));
        if (client.checkExists().forPath(parent) == null) {
            client.create().creatingParentsIfNeeded().forPath(parent);
        }
    }

    /**
     * URL 编码（ZK 节点名不能包含 / ? & 等字符）
     */
    private String encodeUrl(String url) {
        try {
            return URLEncoder.encode(url, "UTF-8");
        } catch (Exception e) {
            return url;
        }
    }
}
