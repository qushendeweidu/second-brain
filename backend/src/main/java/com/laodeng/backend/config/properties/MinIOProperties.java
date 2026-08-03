package com.laodeng.backend.config.properties;

import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/8/3 23:33
 * @description MinIO配置类
 */

@Data
@ConfigurationProperties(prefix = "minio")
public class MinIOProperties {
    private String endpoint;
    private String fileHost;
    private String bucketName;
    private String accessKey;
    private String secretKey;
}
