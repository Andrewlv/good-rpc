package com.andrew.goodrpc.config;

import lombok.Data;

/**
 * RPC 注册中心配置
 */
@Data
public class RegistryConfig {

    /**
     * 注册中心类别
     */
    private String registry = "etcd";

    /**
     * 注册中心地址（etcd 客户端端口是 2379；2380 是集群内部通信的 peer 端口）
     */
    private String address = "http://localhost:2379";

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 超时时间
     */
    private Long timeout = 50000L;
}
