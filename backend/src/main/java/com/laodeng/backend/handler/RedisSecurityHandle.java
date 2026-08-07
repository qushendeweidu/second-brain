package com.laodeng.backend.handler;

import com.laodeng.backend.common.ErrorCode;
import com.laodeng.backend.exception.BusinessException;
import com.laodeng.backend.exception.ThrowUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
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
    private static final long TOKEN_TTL_SECONDS = 30L * 24 * 60 * 60;

    static {
        //使用内置的Lua语言实现操作原子性
        CREATE_SECURITY_KEY_SCRIPT.setScriptText(
                "local current = redis.call('GET', KEYS[1])\n"
                        + "if current == ARGV[1] then\n"
                        + "    return 0\n"
                        + "end\n"
                        + "redis.call('SET', KEYS[1], ARGV[2], 'EX', " + TOKEN_TTL_SECONDS + ")\n"
                        + "return 1");
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
        createSecurityKey(key, value, null);
    }

    /**
     * 创建令牌时修改过期时间
     * @param key
     * @param value
     * @param ttl
     */
    public void createSecurityKey(String key, String value, Duration ttl) {
        try {
            key = decorateKey(key);
            Long result = this.securityRedisTemplate.execute(
                    CREATE_SECURITY_KEY_SCRIPT,
                    List.of(key),
                    BLOCKED_FLAG,   // ARGV[1]
                    value);         // ARGV[2]
            ThrowUtils.throwIf(result != null && result == 0L, ErrorCode.USER_BLOCKED);
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
