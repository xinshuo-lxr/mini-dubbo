package org.apache.dubbo.common.utils;/**
 * @description: TODO
 * @author 陆向荣
 * @date 2026/6/4 22:47
 * @version 1.0
 */
public class StringUtils {
    /**
     * is empty string.
     *
     * @param str source string.
     * @return is empty.
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }
}
