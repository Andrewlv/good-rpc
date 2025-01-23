package com.andrew.goodrpc.registry;

import com.andrew.goodrpc.config.RegistryConfig;
import com.andrew.goodrpc.model.ServiceMetaInfo;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 服务注册中心
 */
public interface Registry {

    /**
     * 初始化服务注册中心
     *
     * @param registryConfig
     */
    void init(RegistryConfig registryConfig);

    /**
     * 服务注册
     *
     * @param serviceMetaInfo
     */
    void register(ServiceMetaInfo serviceMetaInfo) throws ExecutionException, InterruptedException, Exception;

    /**
     * 服务注销
     *
     * @param serviceMetaInfo
     */
    void unRegister(ServiceMetaInfo serviceMetaInfo);

    /**
     * 服务发现
     *
     * @param serviceKey
     * @return
     */
    List<ServiceMetaInfo> serviceDiscovery(String serviceKey);

    /**
     * 服务销毁
     */
    void destroy();

    /**
     * 服务心跳
     */
    void heartBeat();

    /**
     * 监听服务节点变化
     *
     * @param serviceNodeKey
     */
    void watch(String serviceNodeKey);
}
