package org.apache.dubbo.remoting;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * 编解码器 — 把 Request/Response 序列化为字节数组。
 *
 * 简化版：使用 JDK 序列化。
 * 真实 Dubbo 使用自定义协议头（魔数 + 消息类型 + 序列化类型 + 请求ID + 数据长度 + 数据体）。
 *
 * 对应 Dubbo 源码：org.apache.dubbo.rpc.protocol.dubbo.DubboCodec
 */
public class DubboCodec {

    /**
     * 编码：对象 → 字节数组
     */
    public static byte[] encode(Object obj) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(obj);
        oos.flush();
        return bos.toByteArray();
    }

    /**
     * 解码：字节数组 → 对象
     */
    public static Object decode(byte[] data) throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bis);
        return ois.readObject();
    }
}
