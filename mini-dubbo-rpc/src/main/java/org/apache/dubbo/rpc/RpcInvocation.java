package org.apache.dubbo.rpc;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Invocation 的实现类 — 会序列化发送到远端。
 *
 * 关键区分：
 * - attachments：会网络传输（path、version、timeout 等）
 * - attributes：仅本地使用（不序列化）
 *
 * 对应 Dubbo 源码：org.apache.dubbo.rpc.RpcInvocation
 */
public class RpcInvocation implements Invocation, Serializable {

    private static final long serialVersionUID = 1L;

    private String methodName;
    private String serviceName;
    private Class<?>[] parameterTypes;
    private Object[] arguments;

    /** 附件 — 会发送到远端 */
    private Map<String, Object> attachments = new HashMap<>();

    /** 属性 — 仅本地，不序列化 */
    private transient Map<Object, Object> attributes = new HashMap<>();

    /** 反向引用 */
    private transient Invoker<?> invoker;

    public RpcInvocation() {}

    public RpcInvocation(String methodName, String serviceName, Class<?>[] parameterTypes, Object[] arguments) {
        this.methodName = methodName;
        this.serviceName = serviceName;
        this.parameterTypes = parameterTypes;
        this.arguments = arguments;
    }

    @Override public String getMethodName() { return methodName; }
    @Override public String getServiceName() { return serviceName; }
    @Override public Class<?>[] getParameterTypes() { return parameterTypes; }
    @Override public Object[] getArguments() { return arguments; }
    @Override public Map<String, Object> getAttachments() { return attachments; }
    @Override public Map<Object, Object> getAttributes() { return attributes; }
    @Override public Invoker<?> getInvoker() { return invoker; }
    @Override public void setInvoker(Invoker<?> invoker) { this.invoker = invoker; }

    public void setAttachment(String key, String value) { attachments.put(key, value); }
    public String getAttachment(String key) { return (String) attachments.get(key); }

    @Override
    public String toString() {
        return "RpcInvocation [methodName=" + methodName + ", serviceName=" + serviceName + "]";
    }
}
