package com.andrew.example.provider;


import com.andrew.example.common.service.UserService;
import com.andrew.goodrpc.RpcApplication;
import com.andrew.goodrpc.config.RegistryConfig;
import com.andrew.goodrpc.config.RpcConfig;
import com.andrew.goodrpc.model.ServiceMetaInfo;
import com.andrew.goodrpc.registry.LocalRegistry;
import com.andrew.goodrpc.registry.Registry;
import com.andrew.goodrpc.registry.RegistryFactory;
import com.andrew.goodrpc.server.HttpServer;
import com.andrew.goodrpc.server.tcp.VertxTcpServer;

/**
 * 服务提供者示例
 */
public class ProviderExample {

    public static void main(String[] args) {
        // RPC 框架初始化
        RpcApplication.init();

        // 注册服务
        String serviceName = UserService.class.getName();
        LocalRegistry.register(serviceName, UserServiceImpl.class);

        // 注册服务到注册中心
        RpcConfig rpcConfig = RpcApplication.getRpcConfig();
        RegistryConfig registryConfig = rpcConfig.getRegistryConfig();
        Registry registry = RegistryFactory.getInstance(registryConfig.getRegistry());
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName(serviceName);
        serviceMetaInfo.setServiceHost(rpcConfig.getServerHost());
        serviceMetaInfo.setServicePort(rpcConfig.getServerPort());
        try {
            registry.register(serviceMetaInfo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 启动 web 服务
        HttpServer vertxTcpServer = new VertxTcpServer();
        vertxTcpServer.doStart(RpcApplication.getRpcConfig().getServerPort());
    }
}
