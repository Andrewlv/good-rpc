package com.andrew.goodrpc.server.tcp;

import com.andrew.goodrpc.model.RpcRequest;
import com.andrew.goodrpc.model.RpcResponse;
import com.andrew.goodrpc.protocol.*;
import com.andrew.goodrpc.registry.LocalRegistry;
import com.andrew.goodrpc.server.HttpServer;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Method;

@Slf4j
public class VertxTcpServer implements HttpServer {

    @Override
    public void doStart(int port) {
        // 创建 Vert.x 实例
        Vertx vertx = Vertx.vertx();

        // 创建 TCP 服务器
        NetServer server = vertx.createNetServer();

        // 处理请求
        server.connectHandler(socket -> {
            // 构造parser（和消费端一致：先读17字节消息头，再按消息体长度读取，解决粘包/半包问题）
            TcpBufferHandlerWrapper handlerWrapper = new TcpBufferHandlerWrapper(buffer -> {
                // 处理请求并写回响应
                handleRequest(buffer, socket);
            });
            socket.handler(handlerWrapper);
        });

        // 启动 TCP 服务器并监听指定端口
        server.listen(port, result -> {
            if (result.succeeded()) {
                log.info("TCP server started on port " + port);
            } else {
                log.info("Failed to start TCP server: " + result.cause());
            }
        });
    }

    /**
     * 处理请求：解码请求 => 反射调用服务 => 编码响应并写回连接
     *
     * @param resultBuffer
     * @param socket
     */
    private void handleRequest(Buffer resultBuffer, NetSocket socket) {
        // 解码请求
        ProtocolMessage<RpcRequest> protocolMessage;
        try {
            protocolMessage = (ProtocolMessage<RpcRequest>) ProtocolMessageDecoder.decode(resultBuffer);
        } catch (IOException e) {
            log.error("协议消息解码错误", e);
            return;
        }
        RpcRequest rpcRequest = protocolMessage.getBody();

        // 构造响应结果对象
        RpcResponse rpcResponse = new RpcResponse();
        // 如果请求为null 直接返回
        if (rpcRequest == null) {
            rpcResponse.setMessage("Rpc Request is null");
            doResponse(socket, protocolMessage.getHeader(), rpcResponse);
            return;
        }

        try {
            // 获取要调用的服务实现类，通过反射调用
            Class<?> implClass = LocalRegistry.get(rpcRequest.getServiceName());
            Method method = implClass.getMethod(rpcRequest.getMethodName(), rpcRequest.getParameterTypes());
            Object result = method.invoke(implClass.newInstance(), rpcRequest.getParameters());
            // 封装返回结果
            rpcResponse.setData(result);
            rpcResponse.setDataType(method.getReturnType());
            rpcResponse.setMessage("ok");
        } catch (Exception e) {
            // 调用异常时也要返回携带异常信息的响应，否则消费端会一直挂起等待
            log.error("服务调用失败", e);
            rpcResponse.setMessage(e.getMessage());
            rpcResponse.setException(e);
        }

        // 响应
        doResponse(socket, protocolMessage.getHeader(), rpcResponse);
    }

    /**
     * 响应：复用请求头信息构造响应消息，编码后写回连接
     *
     * @param socket
     * @param requestHeader
     * @param rpcResponse
     */
    private void doResponse(NetSocket socket, ProtocolMessage.Header requestHeader, RpcResponse rpcResponse) {
        // 构造响应消息头（序列化器和请求保持一致，请求id对应）
        ProtocolMessage.Header header = new ProtocolMessage.Header();
        header.setMagic(ProtocolConstant.PROTOCOL_MAGIC);
        header.setVersion(ProtocolConstant.PROTOCOL_VERSION);
        header.setSerializer(requestHeader.getSerializer());
        header.setType((byte) ProtocolMessageTypeEnum.RESPONSE.getKey());
        header.setStatus((byte) ProtocolMessageStatusEnum.OK.getValue());
        header.setRequestId(requestHeader.getRequestId());

        ProtocolMessage<RpcResponse> responseMessage = new ProtocolMessage<>();
        responseMessage.setHeader(header);
        responseMessage.setBody(rpcResponse);

        // 编码响应并写回连接
        try {
            Buffer encodeBuffer = ProtocolMessageEncoder.encode(responseMessage);
            socket.write(encodeBuffer);
        } catch (Exception e) {
            log.error("协议消息编码错误", e);
        }
    }

    public static void main(String[] args) {
        new VertxTcpServer().doStart(8888);
    }
}
