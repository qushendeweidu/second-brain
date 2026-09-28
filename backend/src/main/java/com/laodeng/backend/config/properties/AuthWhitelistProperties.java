package com.laodeng.backend.config.properties;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/9/28 11:23
 * @description 权限校验白名单配置类
 */

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 鉴权白名单配置，permitAll 和 JWT 过滤器共用这一份
 */
@Data
@ConfigurationProperties(prefix = "app.security")
public class AuthWhitelistProperties {
    private List<String> whitelist = new ArrayList<>();
}
