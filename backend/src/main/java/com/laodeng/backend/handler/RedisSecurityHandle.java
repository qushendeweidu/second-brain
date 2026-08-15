package com.laodeng.backend.handler;

import com.laodeng.backend.common.ErrorCode;
import com.laodeng.backend.exception.BusinessException;
import com.laodeng.backend.exception.ThrowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/7/31 17:54
 * @description Redis安全令牌处理类
 */

@Component
public class RedisSecurityHandle {
    private final RedisTemplate<String, String> securityRedisTemplate;
    private static final DefaultRedisScript<Long> CREATE_SECURITY_KEY_SCRIPT = new DefaultRedisScript<>();
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
    }

    @Autowired
    public RedisSecurityHandle(@Qualifier("securityRedisTemplate") RedisTemplate<String, String> securityRedisTemplate) {
        this.securityRedisTemplate = securityRedisTemplate;
    }

    /**
     * 创建安全令牌
     * @param key
     * @param value
     */
    public void createSecurityKey(String key, String value) {
        createSecurityKey(key, value, DEFAULT_TTL_DAYS, TimeUnit.DAYS);
    }

    /**
     * 创建令牌时修改过期时间
     * @param key
     * @param value
     * @param ttl
     */
    public void createSecurityKey(String key, String value, Long ttl, TimeUnit timeUnit) {
        try {
            key = decorateKey(key);
            Long seconds = Math.max(1, timeUnit.toSeconds(ttl));
            Long result = this.securityRedisTemplate.execute(
                    CREATE_SECURITY_KEY_SCRIPT,
                    List.of(key),
                    BLOCKED_FLAG,   // ARGV[1]
                    value,   // ARGV[2]
                    String.valueOf(seconds));    // ARGV[3]
            ThrowUtils.throwIf(result == 0L, ErrorCode.USER_BLOCKED);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 删除安全令牌
     * @param key
     */
    public void deleteSecurityKey(String key) {
        try{
            key = decorateKey(key);
            this.securityRedisTemplate.delete(key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取安全令牌
     * @param key
     * @return
     */
    public String getSecurityKey(String key) {
        try{
            key = decorateKey(key);
            return this.securityRedisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 验证安全令牌
     * @param key
     * @return
     */
    public Boolean checkSecurityKey(String key) {
        try{
            key = decorateKey(key);
            return this.securityRedisTemplate.hasKey(key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 修饰key
     * @param key
     * @return 修改之后的key
     */
    private String decorateKey(String key) {
        return "user:" + key + ":key";
    }

}
