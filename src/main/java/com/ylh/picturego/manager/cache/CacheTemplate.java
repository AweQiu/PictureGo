package com.ylh.picturego.manager.cache;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.DigestUtils;

import java.util.function.Supplier;

@Slf4j
public abstract class CacheTemplate {

    /**
     * 从缓存中获取数据，如不存在则返回null
     *
     * @param cacheKeyPrefix 缓存key前缀
     * @param queryRequest   查询请求
     * @param typeReference  返回值类型
     * @param <T>            返回值类型
     * @return 缓存数据
     */
    public final <T> T getCacheValue(String cacheKeyPrefix,
                                     Object queryRequest,
                                     TypeReference<T> typeReference) {
        String cacheKey = buildCacheKey(cacheKeyPrefix, queryRequest);
        String cached = getFromCache(cacheKey);
        if (cached == null) {
            return null;
        }
        try {
            return JSONUtil.toBean(cached, typeReference, false);
        } catch (Exception e) {
            log.warn("缓存反序列化失败，视为未命中：key={}", cacheKey, e);
            return null;
        }
    }

    /**
     * 从缓存中获取数据，如不存在则执行dbLoader
     *
     * @param cacheKeyPrefix 缓存key前缀
     * @param queryRequest   查询请求
     * @param typeReference  返回值类型
     * @param dbLoader       数据加载器
     * @param <T>            返回值类型
     * @return 缓存数据
     */
    public final <T> T getCacheValue(String cacheKeyPrefix,
                               Object queryRequest,
                               TypeReference<T> typeReference,
                               Supplier<T> dbLoader) {
        T cached = getCacheValue(cacheKeyPrefix, queryRequest, typeReference);
        if (cached != null) {
            return cached;
        }
        T loaded = dbLoader.get();
        putCacheValue(cacheKeyPrefix, queryRequest, loaded);
        return loaded;
    }

    /**
     * 构建缓存key
     *
     * @param cacheKeyPrefix 缓存key前缀
     * @param queryRequest   查询请求
     * @return 缓存key
     */
    protected String buildCacheKey(String cacheKeyPrefix, Object queryRequest) {
        String queryCondition = JSONUtil.toJsonStr(queryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        return String.format("picture:%s:%s", cacheKeyPrefix, hashKey);
    }

    /**
     * 将数据放入缓存
     *
     * @param cacheKeyPrefix 缓存key前缀
     * @param queryRequest   查询请求
     * @param value          缓存值
     */
    public final void putCacheValue(String cacheKeyPrefix,
                                    Object queryRequest,
                                    Object value) {
        if (value == null) {
            return;
        }
        String cacheKey = buildCacheKey(cacheKeyPrefix, queryRequest);
        putToCache(cacheKey, JSONUtil.toJsonStr(value));
    }

    // 抽象方法，具体实现由子类完成
    protected abstract String getFromCache(String key);

    // 抽象方法，具体实现由子类完成
    protected abstract void putToCache(String key, String value);
}
