package com.laodeng.backend.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/8/2 19:29
 * @description
 */

@Data
@ConfigurationProperties(prefix = "frontend")
public class FrontendProperties {

    private String url;

    private Long maxAge;
}
