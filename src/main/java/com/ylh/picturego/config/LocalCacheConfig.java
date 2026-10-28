package com.ylh.picturego.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LocalCacheConfig {

    /**
     * 本地缓存
     */
    @Bean("localCache")
    public Cache<String, String> localCache() {
        return Caffeine.newBuilder()
                .initialCapacity(1024)
                .maximumSize(10_000L) // 最大缓存条数
                .expireAfterWrite(Duration.ofMinutes(5)) // 缓存过期时间5分钟
                .build();
    }
}
