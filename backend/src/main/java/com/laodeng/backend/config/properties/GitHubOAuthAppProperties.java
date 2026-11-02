package com.laodeng.backend.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/10/13 00:04
 * @description GitHubOAuthApp配置类
 */

@Data
@ConfigurationProperties(prefix = "github.oauth")
public class GitHubOAuthAppProperties {

    private String clientId;

    private String clientSecret;

    private String redirectUri;

    private String proxyHost;

    private String proxyPort;

}
