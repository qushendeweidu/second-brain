package com.laodeng.backend.config;

import com.laodeng.backend.config.properties.RedisProperties;
import com.laodeng.backend.factory.RedisTemplateFactory;
import io.lettuce.core.resource.DefaultClientResources;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/7/28 11:52
 * @description
 */

@Log4j2
@Configuration
public class RedisConfig {
    private final RedisProperties redisProperties;
    private final DefaultClientResources clientResources;

    @Autowired
    public RedisConfig(RedisProperties redisProperties, DefaultClientResources clientResources) {
        this.redisProperties = redisProperties;
        this.clientResources = clientResources;
    }
    @Bean(value = "securityRedisTemplate")
    public RedisTemplate<String, String> securityRedisTemplate() {
        RedisTemplateFactory<String> redisTemplateFactory = new RedisTemplateFactory<>(String.class);
        LettuceConnectionFactory lettuceConnectionFactory = redisTemplateFactory.createConnectionFactory(this.redisProperties.getSources().get("security"), this.clientResources);
        return redisTemplateFactory.createTemplate(lettuceConnectionFactory);
    }

    @Bean(value = "hotDataRedisTemplate")
    public RedisTemplate<String, String> hotDataRedisTemplate() {
        RedisTemplateFactory<String> redisTemplateFactory = new RedisTemplateFactory<>(String.class);
        LettuceConnectionFactory lettuceConnectionFactory = redisTemplateFactory.createConnectionFactory(this.redisProperties.getSources().get("hot-data"), this.clientResources);
        return redisTemplateFactory.createTemplate(lettuceConnectionFactory);
    }

}
