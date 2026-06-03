package org.apache.dubbo.rpc;

import java.io.Serializable;

/**
 * Result 的实现类 — 包装返回值或异常。
 *
 * 对应 Dubbo 源码：org.apache.dubbo.rpc.AppResponse
 */
public class AppResponse implements Result, Serializable {

    private static final long serialVersionUID = 1L;

    private Object value;
    private Throwable exception;

    public AppResponse() {}

    public AppResponse(Object value) {
        this.value = value;
    }

    public AppResponse(Throwable exception) {
        this.exception = exception;
    }

    @Override public Object getValue() { return value; }
    @Override public Throwable getException() { return exception; }
    @Override public boolean hasException() { return exception != null; }

    public void setValue(Object value) { this.value = value; }
    public void setException(Throwable exception) { this.exception = exception; }
}
