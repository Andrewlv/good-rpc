package com.andrew.goodrpc.server.tcp;

import com.andrew.goodrpc.server.HttpServer;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VertxTcpServer implements HttpServer {

    private byte[] handleRequest(byte[] requestData) {
        //todo
        return "Hello, Client!".getBytes();
    }

    @Override
    public void doStart(int port) {

        // 创建Vert.x实例
        Vertx vertx = Vertx.vertx();

        // 创建TCP服务器
        NetServer server = vertx.createNetServer();

        // 处理请求
        server.connectHandler(socket -> {
            // 处理连接
            socket.handler(buffer -> {
                // 处理收到的字节数组
                byte[] requestData = buffer.getBytes();
                // 在这里自定义字节数组的处理逻辑，比如解析请求、调用服务、构造响应等
                byte[] responseData = handleRequest(requestData);
                // 发送响应
                socket.write(Buffer.buffer(responseData));
            });
        });

        server.listen(port, result -> {
            if (result.succeeded()) {
                log.info("TCP server started on port {}", port);
            } else {
                log.info("Failed to start TCP server", result.cause());
            }
        });
    }

    public static void main(String[] args) {
        new VertxTcpServer().doStart(8888);
    }
}
