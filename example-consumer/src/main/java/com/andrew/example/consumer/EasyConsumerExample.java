package com.andrew.example.consumer;

import com.andrew.example.common.model.User;
import com.andrew.example.common.service.UserService;
import com.andrew.goodrpc.proxy.ServiceProxyFactory;

/**
 * 简易服务消费者示例
 */
public class EasyConsumerExample {
    public static void main(String[] args) {

        UserService userService = ServiceProxyFactory.getProxy(UserService.class);
        User user = new User();
        user.setName("John");

        User newUser = userService.getUser(user);
        if (newUser != null) {
            System.out.println(newUser.getName());
        } else {
            System.out.println("User not found");
        }
    }
}
