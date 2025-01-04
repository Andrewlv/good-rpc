package com.andrew.example.consumer;

import com.andrew.example.common.model.User;
import com.andrew.example.common.service.UserService;
import com.andrew.goodrpc.proxy.ServiceProxyFactory;

/**
 * 服务消费者示例
 */
public class ConsumerExample {

    public static void main(String[] args) {

        // 获取代理
        UserService userService = ServiceProxyFactory.getProxy(UserService.class);
        User user = new User();
        user.setName("Andrew");
        // 调用
        User newUser = userService.getUser(user);
        if (newUser != null) {
            System.out.println(newUser.getName());
        } else {
            System.out.println("user == null");
        }
        int number = userService.getNumber();
        System.out.println(number);
    }
}
