package org.apache.dubbo.common.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * 网络工具类 — 获取本机 IP 地址。
 *
 * 对应 Dubbo 源码：org.apache.dubbo.common.utils.NetUtils
 */
public class NetUtils {

    private static volatile String LOCAL_HOST;

    /**
     * 获取本机 IP 地址（非 127.0.0.1 的第一个有效地址）
     *
     * 对应 Dubbo 源码：NetUtils.getLocalHost()
     */
    public static String getLocalHost() {
        if (LOCAL_HOST != null) {
            return LOCAL_HOST;
        }
        InetAddress address = getLocalAddress();
        LOCAL_HOST = address != null ? address.getHostAddress() : "127.0.0.1";
        return LOCAL_HOST;
    }

    /**
     * 获取本机 InetAddress（优先非回环、非虚拟网卡的地址）
     */
    private static InetAddress getLocalAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || ni.isVirtual() || !ni.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress() && address instanceof java.net.Inet4Address) {
                        return address;
                    }
                }
            }
        } catch (SocketException e) {
            // ignore
        }
        try {
            return InetAddress.getLocalHost();
        } catch (Exception e) {
            return null;
        }
    }
}
