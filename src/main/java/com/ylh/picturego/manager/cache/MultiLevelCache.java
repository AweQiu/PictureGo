package com.ylh.picturego.manager.cache;

import cn.hutool.core.lang.TypeReference;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class MultiLevelCache {

    @Resource
    private LocalCacheOperation localCacheOperation;

    @Resource
    private RedisCacheOperation redisCacheOperation;

    /**
     * 获取缓存值，先从本地缓存获取，如果不存在则从Redis缓存获取
     *
     * @param cacheKeyPrefix 缓存键前缀
     * @param queryRequest 查询请求
     * @param typeReference 返回值类型
     * @return 缓存值
     */
    public <T> T getCacheValue(
            String cacheKeyPrefix,
            Object queryRequest,
            TypeReference<T> typeReference
    ) {
        // 从本地缓存获取缓存值
        T localValue = localCacheOperation.getCacheValue(cacheKeyPrefix, queryRequest, typeReference);
        if (localValue != null) {
            return localValue;
        }
        // 从Redis缓存获取缓存值
        T redisValue = redisCacheOperation.getCacheValue(cacheKeyPrefix, queryRequest, typeReference);
        if (redisValue != null) {
            // 将Redis缓存的值放入本地缓存
            localCacheOperation.putCacheValue(cacheKeyPrefix, queryRequest, redisValue);
            return redisValue;
        }
        return null;
    }

    /**
     * 同时回填本地缓存和Redis缓存
     *
     * @param cacheKeyPrefix 缓存键前缀
     * @param queryRequest 查询请求
     * @param value 缓存值
     */
    public <T> void putCacheValue(
            String cacheKeyPrefix,
            Object queryRequest,
            T value
    ) {
        localCacheOperation.putCacheValue(cacheKeyPrefix, queryRequest, value);
        redisCacheOperation.putCacheValue(cacheKeyPrefix, queryRequest, value);
    }
}
