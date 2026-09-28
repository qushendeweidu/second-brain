package com.laodeng.backend.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/9/16 21:32
 * @description
 */

@Component
public class RedisDataHandle {
    private final RedisTemplate<String, String> hotDataRedisTemplate;

    @Autowired
    public RedisDataHandle( RedisTemplate<String, String> hotDataRedisTemplate) {
        this.hotDataRedisTemplate = hotDataRedisTemplate;
    }

    /**
     * maxRetries = 3 → 首次调用 + 3次重试 = 最多4次
     * delay = 500ms，multiplier (下次重拾延迟时间的倍数)= 2.0 → 退避序列：500ms, 1000ms, 2000ms
     * jitter = 100ms → 实际延迟在 [delay±jitter] 范围内随机，防止同时重试炸掉redis
     * maxDelay = 5000ms → 上限封顶
     * includes → 只对连接/系统异常重试，序列化异常不重试
     */

    /**
     * 创建或者更新redis的数据
     * @param key
     * @param value
     */
    @Retryable(
            includes = {RedisConnectionFailureException.class, RedisSystemException.class},
            maxRetries = 3,
            delay = 500,
            multiplier = 2.0,
            jitter = 100,
            maxDelay = 5000,
            timeUnit = TimeUnit.MILLISECONDS
    )
    public void createOrUpdateHotData(String key, String value) {
        this.hotDataRedisTemplate.opsForValue().set(key,value);
    }

    /**
     * 删除redis中的数据
     * @param key
     */
    @Retryable(
            includes = {RedisConnectionFailureException.class, RedisSystemException.class},
            maxRetries = 3,
            delay = 500,
            multiplier = 2.0,
            jitter = 100,
            maxDelay = 5000,
            timeUnit = TimeUnit.MILLISECONDS
    )
    public void deleteHotData(String key) {
        this.hotDataRedisTemplate.delete(key);
    }


    /**
     * 获取数据
     * @param key
     * @return
     */
    @Retryable(
            includes = {RedisConnectionFailureException.class, RedisSystemException.class},
            maxRetries = 3,
            delay = 500,
            multiplier = 2.0,
            jitter = 100,
            maxDelay = 5000,
            timeUnit = TimeUnit.MILLISECONDS
    )
    public String getHotData(String key) {
        return this.hotDataRedisTemplate.opsForValue().get(key);
    }


}
