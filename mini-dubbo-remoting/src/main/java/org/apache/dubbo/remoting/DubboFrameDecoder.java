package org.apache.dubbo.remoting;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * Dubbo 协议帧解码器 — 解决 TCP 粘包/半包问题。
 *
 * 继承 Netty 的 LengthFieldBasedFrameDecoder，根据协议头中的 dataLength 字段
 * 自动等待完整消息到达后再交给下游 handler 处理。
 *
 * 协议格式：
 * +--------+--------+--------+--------+--------+--------+--------+--------+
 * | magic(high) | magic(low) | type   |            request id           |
 * +--------+--------+--------+--------+--------+--------+--------+--------+
 * |                request id (cont)            |       data length      |
 * +--------+--------+--------+--------+--------+--------+--------+--------+
 * |                              data                                   |
 * +--------+--------+--------+--------+--------+--------+--------+--------+
 *
 * 各字段偏移：
 * - bytes 0-1:  magic (2 bytes)
 * - byte  2:    type (1 byte)
 * - bytes 3-10: id (8 bytes)
 * - bytes 11-14: dataLength (4 bytes)
 * - bytes 15+:  data
 */
public class DubboFrameDecoder extends LengthFieldBasedFrameDecoder {

    public DubboFrameDecoder() {
        // maxFrameLength: 单帧最大 8MB，防止恶意大包
        // lengthFieldOffset: 11 (magic 2 + type 1 + id 8)
        // lengthFieldLength: 4
        // lengthAdjustment: 0 (length 字段的值 = data 部分的长度)
        // initialBytesToStrip: 0 (不跳过任何字节，保留完整协议头给下游解码)
        super(8 * 1024 * 1024, 11, 4, 0, 0);
    }
}
