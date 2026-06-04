package org.apache.dubbo.remoting;

import java.io.Serializable;

/**
 * RPC 响应对象 — Provider 返回给 Consumer。
 *
 * 包含：响应 ID（匹配请求）+ 结果（AppResponse）+ 错误信息
 *
 * 对应 Dubbo 源码：org.apache.dubbo.remoting.exchange.Response
 */
public class Response implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 响应 ID，与 Request.id 匹配 */
    private long id;

    /** 调用结果（AppResponse） */
    private Object result;

    /** 错误信息 */
    private String errorMsg;

    public Response() {}
    public Response(long id) { this.id = id; }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public Object getResult() { return result; }
    public void setResult(Object result) { this.result = result; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
}
