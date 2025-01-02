package com.andrew.example.consumer;

import com.andrew.goodrpc.config.RpcConfig;
import com.andrew.goodrpc.utils.ConfigUtils;

/**
 * 服务消费者示例
 */
public class ConsumerExample {

    public static void main(String[] args) {
        RpcConfig rpcConfig = ConfigUtils.loadConfig(RpcConfig.class, "rpc");
        System.out.println(rpcConfig);
    }
}
