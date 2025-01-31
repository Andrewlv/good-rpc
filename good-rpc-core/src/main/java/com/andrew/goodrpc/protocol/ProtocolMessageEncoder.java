package com.andrew.goodrpc.protocol;

import com.andrew.goodrpc.serializer.Serializer;
import com.andrew.goodrpc.serializer.SerializerFactory;
import io.vertx.core.buffer.Buffer;

/**
 * 协议消息编码器
 */
public class ProtocolMessageEncoder {

    /**
     * 编码
     *
     * @param protocolMessage
     * @return
     * @throws Exception
     */
    public static Buffer encode(ProtocolMessage<?> protocolMessage) throws Exception {

        if (protocolMessage == null || protocolMessage.getHeader() == null) {
            return Buffer.buffer();
        }

        // 消息头
        ProtocolMessage.Header header = protocolMessage.getHeader();
        // 依次向缓冲区写入字节
        Buffer buffer = Buffer.buffer();
        buffer.appendByte(header.getMagic());
        buffer.appendByte(header.getVersion());
        buffer.appendByte(header.getSerializer());
        buffer.appendByte(header.getType());
        buffer.appendByte(header.getStatus());
        buffer.appendLong(header.getRequestId());
        // 获取序列化器
        ProtocolMessageSerializerEnum serializerEnum = ProtocolMessageSerializerEnum.getEnumByKey(header.getSerializer());
        if (serializerEnum == null) {
            throw new RuntimeException("serializerEnum is null");
        }
        Serializer serializer = SerializerFactory.getInstance(serializerEnum.getValue());
        byte[] bodyBytes = serializer.serialize(protocolMessage.getBody());
        // 写入body长度
        buffer.appendInt(bodyBytes.length);
        buffer.appendBytes(bodyBytes);
        return buffer;
    }
}
