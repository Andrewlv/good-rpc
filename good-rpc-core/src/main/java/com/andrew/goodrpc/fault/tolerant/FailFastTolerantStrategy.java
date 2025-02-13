package com.andrew.goodrpc.fault.tolerant;

import com.andrew.goodrpc.model.RpcResponse;

import java.util.Map;

/**
 * 快速失败策略 - 容错策略
 */
public class FailFastTolerantStrategy implements TolerantStrategy {


    /**
     * 执行快速失败策略（立刻通知外层调用者）
     *
     * @param context 上下文，用于传递数据
     * @param e       异常
     * @return
     */
    @Override
    public RpcResponse doTolerant(Map<String, Object> context, Exception e) {
        throw new RuntimeException("服务报错", e);
    }
}
