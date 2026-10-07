package com.laodeng.backend.handler;

import com.laodeng.backend.common.ErrorCode;
import com.laodeng.backend.exception.ThrowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/7/31 17:54
 * @description Redis安全令牌处理类
 */

@Component
public class RedisSecurityHandle {
    private final RedisTemplate<String,String> securityRedisTemplate;
    private final RedisTemplate<String,String> restrictRedisTemplate;
    private static final DefaultRedisScript<Long> CREATE_SECURITY_KEY_SCRIPT = new DefaultRedisScript<>();
    private static final DefaultRedisScript<Long> USER_RESTRICT_KEY_SCRIPT = new DefaultRedisScript<>();
    private static final String BLOCKED_FLAG = "0";
    private static final Long DEFAULT_TTL_DAYS = 30L;

    static {
        //使用内置的Lua语言实现操作原子性
        CREATE_SECURITY_KEY_SCRIPT.setScriptText(
                """
                         local current = redis.call('GET', KEYS[1])
                         if current == ARGV[1] then
                             return 0
                         end
                         redis.call('SET', KEYS[1], ARGV[2], 'EX', ARGV[3])
                         return 1
                        """);
        CREATE_SECURITY_KEY_SCRIPT.setResultType(Long.class);

        USER_RESTRICT_KEY_SCRIPT.setScriptText(
                """
                        local key = KEYS[1]
                        local limit = tonumber(ARGV[1])
                        local window = tonumber(ARGV[2])
                        
                        local current = redis.call('INCR', key)
                        if current == 1 then
                            redis.call('EXPIRE', key, window)
                        end
                        
                        if current > limit then
                            return 0
                        else
                            return 1
                        end
                        """
        );
        USER_RESTRICT_KEY_SCRIPT.setResultType(Long.class);
    }

    @Autowired
    public RedisSecurityHandle(
            @Qualifier("securityRedisTemplate") RedisTemplate<String, String> securityRedisTemplate,
            @Qualifier("restrictRedisTemplate") RedisTemplate<String, String> restrictRedisTemplate) {
        this.securityRedisTemplate = securityRedisTemplate;
        this.restrictRedisTemplate = restrictRedisTemplate;
    }

    /**
     * 创建安全令牌
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
    public void createOrUpdateSecurityKey(String key, String value) {
        createOrUpdateSecurityKey(key, value, DEFAULT_TTL_DAYS, TimeUnit.DAYS);
    }

    /**
     * 限流用户的Token的方法
     * @param key
     * @param limit
     * @param window
     * @param timeUnit
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
    public void restrictUserToken(String key, long limit, long window, TimeUnit timeUnit){
        long seconds = Math.max(1,timeUnit.toSeconds(window)); // 将当前过期时间通过时间类型转化成秒
        seconds += ThreadLocalRandom.current().nextLong(0,10);
        key = decorateRestictKey(key,window); // 包装传入的key
        long result = this.restrictRedisTemplate.execute(
                USER_RESTRICT_KEY_SCRIPT,
                List.of(key),
                String.valueOf(limit),
                String.valueOf(seconds)
        );
        ThrowUtils.throwIf(result == 0L ,ErrorCode.USER_RESTRICTED); // 如果结果是0则表示已经超过限制流量
    }

    /**
     * 创建令牌时修改过期时间
     * @param key
     * @param value
     * @param ttl
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
    public void createOrUpdateSecurityKey(String key, String value, Long ttl, TimeUnit timeUnit) {
        key = decorateKey(key);
        Long seconds = Math.max(1, timeUnit.toSeconds(ttl));
        seconds += ThreadLocalRandom.current().nextLong(0,10);
        Long result = this.securityRedisTemplate.execute(
                CREATE_SECURITY_KEY_SCRIPT,
                List.of(key),
                BLOCKED_FLAG,   // ARGV[1]
                value,   // ARGV[2]
                String.valueOf(seconds));    // ARGV[3]
        ThrowUtils.throwIf(result == 0L, ErrorCode.USER_BLOCKED);
    }

    /**
     * 删除安全令牌
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
    public void deleteSecurityKey(String key) {
        key = decorateKey(key);
        this.securityRedisTemplate.delete(key);
    }

    /**
     * 获取安全令牌
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
    public String getSecurityKey(String key) {
        key = decorateKey(key);
        return this.securityRedisTemplate.opsForValue().get(key);
    }

    /**
     * 验证安全令牌
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
    public Boolean checkSecurityKey(String key) {
        key = decorateKey(key);
        return this.securityRedisTemplate.hasKey(key);
    }

    /**
     * 修饰key
     * @param key
     * @return 修改之后的key
     */
    private String decorateKey(String key) {
        return "user:" + key + ":key";
    }

    /**
     * 修饰限流key
     * @param key
     * @return 修改之后的限流key
     */
    private String decorateRestictKey(String key,long windowSeconds) {
        long epochSecond = LocalDateTime.now()
                .atZone(ZoneId.systemDefault()) //从JVM中获取当前系统时区
                .toEpochSecond(); // 将时间自动转化为自计算机元年以来度过的秒数
        long restrictId = epochSecond/windowSeconds; // 直接除法会直接忽略掉小数如果无法整除从而保证了同一限制时间内id不变
        return "restrict:"+ restrictId +":" + key + ":count";
    }

}
