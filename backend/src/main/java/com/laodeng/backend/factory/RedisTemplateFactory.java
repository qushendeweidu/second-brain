package com.laodeng.backend.factory;

import com.laodeng.backend.config.properties.RedisProperties;
import com.laodeng.backend.serializer.GsonRedisSerializer;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.resource.ClientResources;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/7/31 11:49
 * @description RedisTemplate 工厂类，用于创建 RedisTemplate 实例
 */

@Log4j2
public class RedisTemplateFactory<T> {
    private final StringRedisSerializer stringSerializer;
    private final GsonRedisSerializer<T> gsonRedisSerializer;

    public RedisTemplateFactory(Class<T> clazz) {
        this.stringSerializer = new StringRedisSerializer();
        this.gsonRedisSerializer = new GsonRedisSerializer<>(clazz);
    }

    /**
     * 内部通用构建方法
     */
    public LettuceConnectionFactory createConnectionFactory(RedisProperties.RedisSource source, ClientResources clientResources) {
        // 基础连接信息
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(source.getHost()); // redis的Host
        redisConfig.setPort(source.getPort()); // redis的端口号
        redisConfig.setDatabase(source.getDatabase()); // 数据源
        redisConfig.setUsername(source.getUsername()); //redis的用户名
        redisConfig.setPassword(RedisPassword.of(source.getPassword())); // redis的密码

        // 连接池配置
        RedisProperties.Pool poolProp = source.getLettuce().getPool();
        GenericObjectPoolConfig<StatefulConnection<?, ?>> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(poolProp.getMaxActive()); //连接池最大连接数
        poolConfig.setMaxIdle(poolProp.getMaxIdle()); // 最大空闲连接
        poolConfig.setMinIdle(poolProp.getMinIdle()); // 最小空闲连接
        poolConfig.setMaxWait(poolProp.getMaxWait()); // 拿不到连接最长等待时长
        poolConfig.setTimeBetweenEvictionRuns(poolProp.getTimeBetweenEvictionRuns()); // 空闲连接驱逐扫描周期

        // 客户端参数（超时、名称、线程资源）
        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder().poolConfig(poolConfig) // 连接池配置
                .clientName(source.getClientName()) // 客户端名称
                .commandTimeout(source.getTimeout()) // 超时时间
                .shutdownTimeout(source.getLettuce().getShutdownTimeout()) // 断开连接时间
                .clientResources(clientResources) // 客户端资源
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig, clientConfig);
        // 关键点 2：手动创建的工厂必须显式调用 afterPropertiesSet 才会真正初始化连接池！
        factory.afterPropertiesSet();
        return factory;
    }

    public RedisTemplate<String, T> createTemplate(LettuceConnectionFactory lettuceConnectionFactory) {
        RedisTemplate<String, T> template = new RedisTemplate<>();
        template.setKeySerializer(this.stringSerializer);
        template.setHashKeySerializer(this.stringSerializer);
        template.setConnectionFactory(lettuceConnectionFactory);
        template.setValueSerializer(this.gsonRedisSerializer);
        template.setHashValueSerializer(this.gsonRedisSerializer);
        template.afterPropertiesSet(); //让 RedisTemplate 立刻检查并绑定
        return template;
    }


}
