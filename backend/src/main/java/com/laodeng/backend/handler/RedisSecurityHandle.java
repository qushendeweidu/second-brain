package com.laodeng.backend.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/7/31 17:54
 * @description
 */

@Component
public class RedisSecurityHandle {
    private final RedisTemplate<String, String> securityRedisTemplate;

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
        try{
            key = decorateKey(key);
            securityRedisTemplate.opsForValue().set(key, value,30, TimeUnit.DAYS);
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
            securityRedisTemplate.delete(key);
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
            return securityRedisTemplate.opsForValue().get(key);
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
            return securityRedisTemplate.hasKey(key);
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
