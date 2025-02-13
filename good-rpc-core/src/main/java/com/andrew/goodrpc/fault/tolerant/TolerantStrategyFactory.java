package com.andrew.goodrpc.fault.tolerant;

import com.andrew.goodrpc.spi.SpiLoader;

public class TolerantStrategyFactory {

    static {
        SpiLoader.load(TolerantStrategy.class);
    }

    /**
     * 默认策略
     */
    private static final TolerantStrategy DEFAULT_STRATEGY = new FailFastTolerantStrategy();

    /**
     * 获取默认策略
     *
     * @param key
     * @return
     */
    public static TolerantStrategy getInstance(String key) {
        return SpiLoader.getInstance(TolerantStrategy.class, key);
    }
}
