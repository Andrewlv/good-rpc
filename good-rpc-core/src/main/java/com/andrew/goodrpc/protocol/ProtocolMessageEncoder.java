package com.andrew.goodrpc.protocol;

import io.vertx.core.buffer.Buffer;

/**
 * 协议消息编码器
 */
public class ProtocolMessageEncoder {

    public static Buffer encode(ProtocolMessage<?> protocolMessage) {

        if (protocolMessage == null || protocolMessage.getHeader() == null) {
            return Buffer.buffer();
        }


    }
}
