package com.andrew.goodrpc.server.tcp;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VertxTcpClient {

    public void start() {

        // 创建Vert.x实例
        Vertx vertx = Vertx.vertx();

        vertx.createNetClient().connect(8888, "localhost", result -> {
            if (result.succeeded()) {
                log.info("Connected to server");
                NetSocket socket = result.result();
                // 发送数据
                socket.write("Hello, Server!");
                // 接收响应
                socket.handler(buffer -> {
                    log.info("Server received: {}", buffer.toString());
                });
            } else {
                log.error("Connect failed", result.cause());
            }
        });
    }

    public static void main(String[] args) {
        new VertxTcpClient().start();
    }
}
