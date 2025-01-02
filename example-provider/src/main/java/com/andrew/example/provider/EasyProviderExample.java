package com.andrew.example.provider;

import com.andrew.example.common.service.UserService;
import com.andrew.goodrpc.registry.LocalRegistry;
import com.andrew.goodrpc.server.HttpServer;
import com.andrew.goodrpc.server.VertxHttpServer;

/**
 * 简易服务提供者示例
 */
public class EasyProviderExample {

    public static void main(String[] args) {

        // 注册服务
        LocalRegistry.register(UserService.class.getName(), UserServiceImpl.class);

        // 启动web服务
        HttpServer httpServer = new VertxHttpServer();
        httpServer.doStart(8080);
    }
}