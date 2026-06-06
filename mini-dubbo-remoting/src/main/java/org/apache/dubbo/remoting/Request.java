package org.apache.dubbo.remoting;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RPC 请求对象 — Consumer 发送给 Provider。
 *
 * 包含：请求 ID（用于匹配响应）+ 数据（RpcInvocation）
 *
 * 对应 Dubbo 源码：org.apache.dubbo.remoting.exchange.Request
 */
public class Request implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final AtomicLong INVOKE_ID = new AtomicLong(0);

    /** 请求 ID，用于和 Response 匹配 */
    private final long id;

    /** 请求数据（RpcInvocation） */
    private Object data;

    public Request() {
        this.id = INVOKE_ID.getAndIncrement();
    }

    public Request(long id) {
        this.id = id;
    }

    public long getId() { return id; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
}
