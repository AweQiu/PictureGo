package com.ylh.picturego.manager.cache;

import com.github.benmanes.caffeine.cache.Cache;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component("localCacheOperation")
public class LocalCacheOperation extends CacheTemplate{

    @Resource
    private Cache<String, String> localCache;

    @Override
    protected String getFromCache(String key) {
        return localCache.getIfPresent(key);
    }

    @Override
    protected void putToCache(String key, String value) {
        localCache.put(key, value);
    }
}
