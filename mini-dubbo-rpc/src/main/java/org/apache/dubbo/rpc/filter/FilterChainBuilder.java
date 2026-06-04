package org.apache.dubbo.rpc.filter;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.common.URL;

import java.util.Collections;
import java.util.List;

/**
 * Filter 链构建器 — 把 Filter 列表包装成 Invoker 链。
 *
 * 核心思想：每个 Filter 被包装成一个 Invoker（FilterChainNode），
 * 每个 FilterChainNode 持有下一个节点的引用，形成链式调用。
 *
 * 构建过程（逆序遍历）：
 *   last = originalInvoker（最内层，真正的 DubboInvoker）
 *   last = FilterChainNode(filter_C, next=last)
 *   last = FilterChainNode(filter_B, next=last)
 *   last = FilterChainNode(filter_A, next=last)
 *   → 调用顺序：filter_A → filter_B → filter_C → originalInvoker
 *
 * 对应 Dubbo 源码：org.apache.dubbo.rpc.cluster.filter.DefaultFilterChainBuilder
 */
public class FilterChainBuilder {

    /**
     * 构建实例级 Filter 链（Consumer 侧，LoadBalance 之后）
     *
     * @param originalInvoker 最内层的 Invoker（如 DubboInvoker）
     * @param url             URL（用于 SPI 获取 Filter 列表）
     * @return 包装后的 Invoker（最外层是第一个 Filter）
     */
    public static <T> Invoker<T> buildInvokerChain(Invoker<T> originalInvoker, URL url) {
        // 通过 SPI 获取所有 @Activate 的 Filter
        List<Filter> filters = ExtensionLoader.getExtensionLoader(Filter.class)
                .getActivateExtension(url, "filter", "consumer");

        if (filters.isEmpty()) {
            return originalInvoker;
        }

        // 逆序包装：最后一个 Filter 包装 originalInvoker，倒数第二个包装最后一个...
        Invoker<T> last = originalInvoker;
        for (int i = filters.size() - 1; i >= 0; i--) {
            final Filter filter = filters.get(i);
            final Invoker<T> next = last;
            last = new FilterChainNode<>(originalInvoker, next, filter);
        }
        return last;
    }

    /**
     * 构建集群级 ClusterFilter 链（Consumer 侧，LoadBalance 之前）
     */
    public static <T> Invoker<T> buildClusterFilterChain(Invoker<T> originalInvoker, URL url) {
        List<ClusterFilter> filters = ExtensionLoader.getExtensionLoader(ClusterFilter.class)
                .getActivateExtension(url, "cluster-filter", "consumer");

        if (filters.isEmpty()) {
            return originalInvoker;
        }

        Invoker<T> last = originalInvoker;
        for (int i = filters.size() - 1; i >= 0; i--) {
            final ClusterFilter filter = filters.get(i);
            final Invoker<T> next = last;
            last = new ClusterFilterChainNode<>(originalInvoker, next, filter);
        }
        return last;
    }

    /**
     * Filter 链节点 — 本身也是一个 Invoker。
     *
     * invoke() 时调用内部的 filter.invoke(nextNode, invocation)，
     * filter 做完自己的事后再调用 nextNode.invoke(invocation) 传递给下一层。
     */
    private static class FilterChainNode<T> implements Invoker<T> {
        private final Invoker<T> originalInvoker;
        private final Invoker<T> next;
        private final Filter filter;

        public FilterChainNode(Invoker<T> originalInvoker, Invoker<T> next, Filter filter) {
            this.originalInvoker = originalInvoker;
            this.next = next;
            this.filter = filter;
        }

        @Override
        public URL getUrl() { return originalInvoker.getUrl(); }

        @Override
        public Class<T> getInterface() { return originalInvoker.getInterface(); }

        @Override
        public Result invoke(Invocation invocation) throws RpcException {
            // 关键：把 next 传给 filter，filter 调用 next.invoke() 传递给下一层
            return filter.invoke(next, invocation);
        }

        @Override
        public boolean isAvailable() { return originalInvoker.isAvailable(); }

        @Override
        public void destroy() { originalInvoker.destroy(); }

        @Override
        public String toString() {
            return "FilterChainNode [filter=" + filter.getClass().getSimpleName()
                    + ", next=" + next.getClass().getSimpleName() + "]";
        }
    }

    /**
     * ClusterFilter 链节点 — 同样的模式，但包装的是 ClusterFilter
     */
    private static class ClusterFilterChainNode<T> implements Invoker<T> {
        private final Invoker<T> originalInvoker;
        private final Invoker<T> next;
        private final ClusterFilter filter;

        public ClusterFilterChainNode(Invoker<T> originalInvoker, Invoker<T> next, ClusterFilter filter) {
            this.originalInvoker = originalInvoker;
            this.next = next;
            this.filter = filter;
        }

        @Override
        public URL getUrl() { return originalInvoker.getUrl(); }

        @Override
        public Class<T> getInterface() { return originalInvoker.getInterface(); }

        @Override
        public Result invoke(Invocation invocation) throws RpcException {
            return filter.invoke(next, invocation);
        }

        @Override
        public boolean isAvailable() { return originalInvoker.isAvailable(); }

        @Override
        public void destroy() { originalInvoker.destroy(); }

        @Override
        public String toString() {
            return "ClusterFilterChainNode [filter=" + filter.getClass().getSimpleName()
                    + ", next=" + next.getClass().getSimpleName() + "]";
        }
    }
}
