package org.apache.dubbo.remoting;

import io.netty.buffer.ByteBuf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * 编解码器 — Dubbo 私有协议。
 *
 * 协议格式：
 * +--------+--------+--------+--------+--------+--------+--------+--------+
 * | magic  | type   |                request id               |  ...     |
 * +--------+--------+--------+--------+--------+--------+--------+--------+
 * |                      data length                    |      data      |
 * +--------+--------+--------+--------+--------+--------+--------+--------+
 *
 * - magic:       2 字节，固定 0xDABB
 * - type:        1 字节，消息类型（1=Request, 2=Response）
 * - request id:  8 字节，请求 ID
 * - data length: 4 字节，数据体长度
 * - data:        变长，序列化的对象
 *
 * 对应 Dubbo 源码：org.apache.dubbo.rpc.protocol.dubbo.DubboCodec
 */
public class DubboCodec {

    /** 魔数 — 用于识别 Dubbo 协议 */
    public static final short MAGIC = (short) 0xDABB;

    /** 消息类型 */
    public static final byte REQUEST_TYPE = 1;
    public static final byte RESPONSE_TYPE = 2;

    /** 协议头长度：2(magic) + 1(type) + 8(id) + 4(length) = 15 字节 */
    public static final int HEADER_LENGTH = 15;

    /**
     * 编码 Request → ByteBuf
     */
    public static void encodeRequest(ByteBuf out, Request request) throws Exception {
        byte[] data = serialize(request.getData());

        // 写协议头
        out.writeShort(MAGIC);
        out.writeByte(REQUEST_TYPE);
        out.writeLong(request.getId());
        out.writeInt(data.length);

        // 写数据体
        out.writeBytes(data);
    }

    /**
     * 编码 Response → ByteBuf
     */
    public static void encodeResponse(ByteBuf out, Response response) throws Exception {
        // result 和 errorMsg 二选一
        Object data = response.getErrorMsg() != null ? response.getErrorMsg() : response.getResult();
        byte[] bytes = serialize(data);

        out.writeShort(MAGIC);
        out.writeByte(RESPONSE_TYPE);
        out.writeLong(response.getId());
        out.writeInt(bytes.length);
        out.writeBytes(bytes);
    }

    /**
     * 从 ByteBuf 解码协议头
     * @return Header 对象，如果数据不够返回 null
     */
    public static Header decodeHeader(ByteBuf buf) {
        if (buf.readableBytes() < HEADER_LENGTH) {
            return null;
        }

        buf.markReaderIndex();
        short magic = buf.readShort();
        if (magic != MAGIC) {
            buf.resetReaderIndex();
            throw new RuntimeException("Invalid magic number: " + Integer.toHexString(magic & 0xFFFF));
        }

        byte type = buf.readByte();
        long id = buf.readLong();
        int dataLength = buf.readInt();

        return new Header(type, id, dataLength);
    }

    /**
     * 从 ByteBuf 读取数据体
     */
    public static Object decodeData(ByteBuf buf, int dataLength) throws Exception {
        byte[] data = new byte[dataLength];
        buf.readBytes(data);
        return deserialize(data);
    }

    /**
     * JDK 序列化
     */
    private static byte[] serialize(Object obj) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(obj);
        oos.flush();
        return bos.toByteArray();
    }

    /**
     * JDK 反序列化
     */
    private static Object deserialize(byte[] data) throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bis);
        return ois.readObject();
    }

    /**
     * 协议头信息
     */
    public static class Header {
        private final byte type;
        private final long id;
        private final int dataLength;

        public Header(byte type, long id, int dataLength) {
            this.type = type;
            this.id = id;
            this.dataLength = dataLength;
        }

        public byte getType() { return type; }
        public long getId() { return id; }
        public int getDataLength() { return dataLength; }
        public boolean isRequest() { return type == REQUEST_TYPE; }
        public boolean isResponse() { return type == RESPONSE_TYPE; }
    }
}
