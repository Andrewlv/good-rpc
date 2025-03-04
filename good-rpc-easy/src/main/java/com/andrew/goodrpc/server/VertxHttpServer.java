package com.andrew.goodrpc.server;

import io.vertx.core.Vertx;

public class VertxHttpServer implements HttpServer {

    @Override
    public void doStart(int port) {

        // 创建Vert.x实例
        Vertx vertx = Vertx.vertx();
        // 创建Http服务器
        io.vertx.core.http.HttpServer server = vertx.createHttpServer();
        // 监听端口并处理请求
        server.requestHandler(new HttpServerHandler());

        // 启动服务器并监听指定端口
        server.listen(port, result -> {
            if (result.succeeded()) {
                System.out.println("HTTP server started on port " + port);
            } else {
                System.out.println("HTTP server failed to start on port " + port);
            }
        });
    }
}
