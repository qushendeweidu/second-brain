package com.laodeng.backend.config.properties;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/7/28 22:51
 * @description jwt相关配置类
 */

@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private Long expiration;
    private String issuer;
}
