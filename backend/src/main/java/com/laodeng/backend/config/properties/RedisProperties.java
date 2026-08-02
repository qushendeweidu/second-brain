package com.laodeng.backend.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/7/31 14:51
 * @description
 */

@Data
@ConfigurationProperties(prefix = "app.redis")
public class RedisProperties {

    private Map<String,RedisSource> sources = new HashMap<>();

    @Data
    public static class RedisSource{
        private String host;
        private int port;
        private String username;
        private String password;
        private int database;
        private String clientName;
        /** 命令读写超时 */
        private Duration timeout;
        /** TCP 三次握手超时 */
        private Duration connectTimeout;
        private Lettuce lettuce = new Lettuce();
    }

    @Data
    public static class Lettuce {
        private Pool pool = new Pool();
        /** 应用关闭时给 Lettuce 优雅释放的时间，避免丢命令 */
        private Duration shutdownTimeout;
    }

    @Data
    public static class Pool {
        private boolean enabled = true;
        /** 池中最大连接数 */
        private int maxActive;
        /** 最大空闲连接 */
        private int maxIdle;
        /** 最小空闲连接（保活，避免突发流量冷启动慢） */
        private int minIdle;
        /** 拿不到连接时最长等待，-1=无限等 */
        private Duration maxWait;
        /** 空闲连接驱逐扫描周期 */
        private Duration timeBetweenEvictionRuns;
    }

}
