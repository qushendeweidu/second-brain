package com.laodeng.backend.config;

import com.laodeng.backend.config.properties.MinIOProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/7/28 11:50
 * @description web配置
 */

@Log4j2
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final MinIOProperties minIOProperties;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/file/**")
                .addResourceLocations(minIOProperties.getFileHost()+"/"+minIOProperties.getBucketName() + "/");
    }
}
