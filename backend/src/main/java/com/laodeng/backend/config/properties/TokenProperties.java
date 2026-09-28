package com.laodeng.backend.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "token")
public class TokenProperties {
    // 长时Token也叫刷新Token
    private String refresh;
    // 短token也叫普通Token
    private String generation;
}
