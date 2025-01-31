package com.andrew.goodrpc.server;

import com.andrew.goodrpc.server.tcp.TcpBufferHandlerWrapper;
import io.vertx.core.Handler;
import io.vertx.core.net.NetSocket;

/**
 * TCP请求处理
 */
public class TcpServerHandler implements Handler<NetSocket> {


    /**
     * 处理请求
     *
     * @param netSocket
     */
    @Override
    public void handle(NetSocket netSocket) {
        TcpBufferHandlerWrapper tcpBufferHandlerWrapper = new TcpBufferHandlerWrapper(buffer -> {
            // 处理请求代码
        });
        netSocket.handler(tcpBufferHandlerWrapper);
    }
}
