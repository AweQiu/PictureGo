package com.ylh.picturego.manager.cache;

import cn.hutool.core.util.RandomUtil;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component("redisCacheOperation")
public class RedisCacheOperation extends CacheTemplate {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    protected String getFromCache(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    @Override
    protected void putToCache(String key, String value) {
        // 随机设置缓存过期时间，5-10分钟，防止缓存雪崩
        int expireTime = 300 + RandomUtil.randomInt(300, 600);
        stringRedisTemplate.opsForValue().set(key, value, expireTime, TimeUnit.SECONDS);
    }
}
