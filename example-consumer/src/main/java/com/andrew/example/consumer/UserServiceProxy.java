package com.andrew.example.consumer;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.andrew.example.common.model.User;
import com.andrew.example.common.service.UserService;
import com.andrew.goodrpc.model.RpcRequest;
import com.andrew.goodrpc.model.RpcResponse;
import com.andrew.goodrpc.serializer.JdkSerializer;
import com.andrew.goodrpc.serializer.Serializer;

import java.io.IOException;

/**
 * 静态代理
 */
public class UserServiceProxy implements UserService {

    @Override
    public User getUser(User user) {

        // 指定序列化器
        Serializer serializer = new JdkSerializer();

        // 构造请求对象
        RpcRequest rpcRequest = RpcRequest.builder()
                .serviceName(UserService.class.getName())
                .methodName("getUser")
                .parameterTypes(new Class<?>[]{User.class})
                .parameters(new Object[]{user})
                .build();

        // 发请求
        try {
            byte[] bodyBytes = serializer.serialize(rpcRequest);
            byte[] result;
            try (HttpResponse httpResponse = HttpRequest.post("http://localhost:8080")
                    .body(bodyBytes)
                    .execute()) {
                result = httpResponse.bodyBytes();
            }
            RpcResponse rpcResponse = serializer.deserialize(result, RpcResponse.class);
            return (User) rpcResponse.getData();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
