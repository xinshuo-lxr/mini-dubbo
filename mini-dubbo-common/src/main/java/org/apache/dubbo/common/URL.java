package org.apache.dubbo.common;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Dubbo URL — 所有配置信息的唯一载体。
 *
 * 格式：protocol://host:port/path?key1=value1&key2=value2
 *
 * 对应 Dubbo 源码：org.apache.dubbo.common.URL
 * 简化点：去掉 URLAddress/URLParam 压缩存储，直接用 String + HashMap
 */
public class URL implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String protocol;     // "dubbo", "registry", "consumer"
    private final String username;
    private final String password;
    private final String host;
    private final int port;
    private final String path;         // 接口名，如 "org.apache.dubbo.api.demo.DemoService"
    private final Map<String, String> parameters;

    public URL(String protocol, String host, int port, String path, Map<String, String> parameters) {
        this(protocol, null, null, host, port, path, parameters);
    }

    public URL(String protocol, String username, String password, String host, int port, String path, Map<String, String> parameters) {
        this.protocol = protocol;
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = port;
        this.path = path;
        this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
    }

    // ==================== 基础访问 ====================

    public String getProtocol() { return protocol; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getPath() { return path; }
    public Map<String, String> getParameters() { return parameters; }

    // ==================== 参数访问 ====================

    public String getParameter(String key) {
        return parameters.get(key);
    }

    public String getParameter(String key, String defaultValue) {
        String value = parameters.get(key);
        return (value == null || value.isEmpty()) ? defaultValue : value;
    }

    public int getParameter(String key, int defaultValue) {
        String value = parameters.get(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    public boolean getParameter(String key, boolean defaultValue) {
        String value = parameters.get(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    public boolean hasParameter(String key) {
        String value = parameters.get(key);
        return value != null && !value.isEmpty();
    }

    /**
     * 获取方法级参数：先找 methodName.key，找不到再找 key
     */
    public String getMethodParameter(String methodName, String key) {
        String value = parameters.get(methodName + "." + key);
        if (value == null || value.isEmpty()) {
            value = parameters.get(key);
        }
        return value;
    }

    public int getMethodParameter(String methodName, String key, int defaultValue) {
        String value = getMethodParameter(methodName, key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    // ==================== 不可变修改（copy-on-write） ====================

    public URL addParameter(String key, String value) {
        if (key == null || key.isEmpty() || value == null || value.isEmpty()) {
            return this;
        }
        Map<String, String> newParams = new HashMap<>(parameters);
        newParams.put(key, value);
        return new URL(protocol, username, password, host, port, path, newParams);
    }

    public URL addParameterIfAbsent(String key, String value) {
        if (parameters.containsKey(key)) {
            return this;
        }
        return addParameter(key, value);
    }

    public URL removeParameter(String key) {
        if (!parameters.containsKey(key)) {
            return this;
        }
        Map<String, String> newParams = new HashMap<>(parameters);
        newParams.remove(key);
        return new URL(protocol, username, password, host, port, path, newParams);
    }

    public URL addParameters(Map<String, String> newParams) {
        if (newParams == null || newParams.isEmpty()) {
            return this;
        }
        Map<String, String> params = new HashMap<>(parameters);
        params.putAll(newParams);
        return new URL(protocol, username, password, host, port, path, params);
    }

    // ==================== 服务相关 ====================

    /**
     * 获取服务接口名
     */
    public String getServiceInterface() {
        String iface = parameters.get("interface");
        return (iface != null && !iface.isEmpty()) ? iface : path;
    }

    /**
     * 获取服务 Key：group/interface:version
     */
    public String getServiceKey() {
        String group = parameters.get("group");
        String version = parameters.get("version");
        String iface = getServiceInterface();
        StringBuilder key = new StringBuilder();
        if (group != null && !group.isEmpty()) {
            key.append(group).append("/");
        }
        key.append(iface);
        if (version != null && !version.isEmpty()) {
            key.append(":").append(version);
        }
        return key.toString();
    }

    public String getGroup() {
        return parameters.get("group");
    }

    public String getVersion() {
        return parameters.get("version");
    }

    // ==================== 序列化 ====================

    public String toFullString() {
        return buildString(true, true);
    }

    @Override
    public String toString() {
        return buildString(false, true);
    }

    private String buildString(boolean appendUser, boolean appendParameter) {
        StringBuilder buf = new StringBuilder();
        if (protocol != null && !protocol.isEmpty()) {
            buf.append(protocol).append("://");
        }
        if (appendUser && username != null && !username.isEmpty()) {
            buf.append(username);
            if (password != null && !password.isEmpty()) {
                buf.append(":").append(password);
            }
            buf.append("@");
        }
        if (host != null && !host.isEmpty()) {
            buf.append(host);
            if (port > 0) {
                buf.append(":").append(port);
            }
        }
        if (path != null && !path.isEmpty()) {
            buf.append("/").append(path);
        }
        if (appendParameter && !parameters.isEmpty()) {
            boolean first = true;
            for (Map.Entry<String, String> entry : new TreeMap<>(parameters).entrySet()) {
                if (first) {
                    buf.append("?");
                    first = false;
                } else {
                    buf.append("&");
                }
                buf.append(entry.getKey()).append("=").append(entry.getValue());
            }
        }
        return buf.toString();
    }

    // ==================== 静态解析 ====================

    /**
     * 从字符串解析 URL
     * 格式：protocol://host:port/path?key=value&key=value
     */
    public static URL valueOf(String url) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("url == null");
        }
        String protocol = null;
        String username = null;
        String password = null;
        String host = null;
        int port = 0;
        String path = null;
        Map<String, String> parameters = new HashMap<>();

        // 分离协议
        int i = url.indexOf("://");
        if (i > 0) {
            protocol = url.substring(0, i);
            url = url.substring(i + 3);
        }

        // 分离参数
        int j = url.indexOf('?');
        if (j >= 0) {
            String paramStr = url.substring(j + 1);
            url = url.substring(0, j);
            for (String param : paramStr.split("&")) {
                int eq = param.indexOf('=');
                if (eq > 0) {
                    parameters.put(param.substring(0, eq), param.substring(eq + 1));
                }
            }
        }

        // 分离用户名密码
        int k = url.indexOf('@');
        if (k > 0) {
            String userInfo = url.substring(0, k);
            url = url.substring(k + 1);
            int colon = userInfo.indexOf(':');
            if (colon > 0) {
                username = userInfo.substring(0, colon);
                password = userInfo.substring(colon + 1);
            } else {
                username = userInfo;
            }
        }

        // 分离 host:port/path
        int slash = url.indexOf('/');
        if (slash > 0) {
            path = url.substring(slash + 1);
            url = url.substring(0, slash);
        }
        int colon = url.indexOf(':');
        if (colon > 0) {
            host = url.substring(0, colon);
            port = Integer.parseInt(url.substring(colon + 1));
        } else {
            host = url;
        }

        return new URL(protocol, username, password, host, port, path, parameters);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof URL)) return false;
        URL other = (URL) obj;
        return toString().equals(other.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
