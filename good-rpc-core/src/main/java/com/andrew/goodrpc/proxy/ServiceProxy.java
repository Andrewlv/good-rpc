package com.andrew.goodrpc.proxy;

import cn.hutool.core.collection.CollUtil;
import com.andrew.goodrpc.RpcApplication;
import com.andrew.goodrpc.config.RpcConfig;
import com.andrew.goodrpc.constant.RpcConstant;
import com.andrew.goodrpc.fault.retry.RetryStrategy;
import com.andrew.goodrpc.fault.retry.RetryStrategyFactory;
import com.andrew.goodrpc.fault.tolerant.TolerantStrategy;
import com.andrew.goodrpc.fault.tolerant.TolerantStrategyFactory;
import com.andrew.goodrpc.loadbalancer.LoadBalancer;
import com.andrew.goodrpc.loadbalancer.LoadBalancerFactory;
import com.andrew.goodrpc.model.RpcRequest;
import com.andrew.goodrpc.model.RpcResponse;
import com.andrew.goodrpc.model.ServiceMetaInfo;
import com.andrew.goodrpc.registry.Registry;
import com.andrew.goodrpc.registry.RegistryFactory;
import com.andrew.goodrpc.serializer.Serializer;
import com.andrew.goodrpc.serializer.SerializerFactory;
import com.andrew.goodrpc.server.tcp.VertxTcpClient;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务代理（JDK动态代理）
 */
public class ServiceProxy implements InvocationHandler {

    /**
     * 调用代理
     *
     * @param proxy  the proxy instance that the method was invoked on
     * @param method the {@code Method} instance corresponding to
     *               the interface method invoked on the proxy instance.  The declaring
     *               class of the {@code Method} object will be the interface that
     *               the method was declared in, which may be a superinterface of the
     *               proxy interface that the proxy class inherits the method through.
     * @param args   an array of objects containing the values of the
     *               arguments passed in the method invocation on the proxy instance,
     *               or {@code null} if interface method takes no arguments.
     *               Arguments of primitive types are wrapped in instances of the
     *               appropriate primitive wrapper class, such as
     *               {@code java.lang.Integer} or {@code java.lang.Boolean}.
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        // 指定序列化器
        final Serializer serializer = SerializerFactory.getInstance(RpcApplication.getRpcConfig().getSerializer());

        // 构造请求
        String serviceName = method.getDeclaringClass().getName();
        RpcRequest rpcRequest = RpcRequest.builder()
                .serviceName(serviceName)
                .methodName(method.getName())
                .parameterTypes(method.getParameterTypes())
                .parameters(args)
                .build();


        // 从注册中心获取服务提供者请求地址
        RpcConfig rpcConfig = RpcApplication.getRpcConfig();
        Registry registry = RegistryFactory.getInstance(rpcConfig.getRegistryConfig().getRegistry());
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName(serviceName);
        serviceMetaInfo.setServiceVersion(RpcConstant.DEFAULT_SERVICE_VERSION);
        List<ServiceMetaInfo> serviceMetaInfoList = registry.serviceDiscovery(serviceMetaInfo.getServiceKey());
        if (CollUtil.isEmpty(serviceMetaInfoList)) {
            throw new RuntimeException("未找到服务提供者");
        }

        // 负载均衡
        LoadBalancer loadBalancer = LoadBalancerFactory.getInstance(rpcConfig.getLoadBalancer());
        // 将调用方法名作为负载均衡参数
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("methodName", method.getName());
        ServiceMetaInfo selectedService = loadBalancer.select(requestParams, serviceMetaInfoList);

        // 发送 TCP 请求
        // 使用重试机制
        RpcResponse rpcResponse;
        try {
            RetryStrategy retryStrategy = RetryStrategyFactory.getInstance(rpcConfig.getRetryStrategy());
            rpcResponse = retryStrategy.doRetry(() ->
                    VertxTcpClient.doRequest(rpcRequest, selectedService)
            );
        } catch (Exception e) {
            // 容错机制
            TolerantStrategy tolerantStrategy = TolerantStrategyFactory.getInstance(rpcConfig.getRetryStrategy());
            rpcResponse = tolerantStrategy.doTolerant(null, e);
        }
        return rpcResponse.getData();
    }
}
