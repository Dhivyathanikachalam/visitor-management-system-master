package com.java.vms.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisCacheUtil {

    private static final long DEFAULT_REDIS_TTL = 3600; // Default TTL for redis key - 1 hour

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void setValueInRedisWithDefaultTTL(String key, Object value){
        redisTemplate.opsForValue().set(key, value, DEFAULT_REDIS_TTL, TimeUnit.SECONDS);
    }

    public Object getValueFromRedisCache(String key){
        return redisTemplate.opsForValue().get(key);
    }
    
}
