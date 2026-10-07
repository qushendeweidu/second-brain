package com.laodeng.backend.config;

import com.laodeng.backend.config.properties.AuthWhitelistProperties;
import com.laodeng.backend.config.properties.TokenProperties;
import com.laodeng.backend.filter.JwtAuthenticationFilter;
import com.laodeng.backend.handler.RedisSecurityHandle;
import com.laodeng.backend.service.UserService;
import com.laodeng.backend.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/9/22 21:53
 * @description 过滤器配置类
 */
@Configuration
public class FilterConfig {
    private final JwtUtils jwtUtils;
    private final RedisSecurityHandle redisSecurityHandle;
    private final List<String> authWhitelist;
    private final TokenProperties tokenProperties;

    @Autowired
    public FilterConfig(JwtUtils jwtUtils,
                        RedisSecurityHandle redisSecurityHandle,
                        TokenProperties tokenProperties,
                        AuthWhitelistProperties authWhitelistProperties) {
        this.jwtUtils = jwtUtils;
        this.redisSecurityHandle = redisSecurityHandle;
        this.authWhitelist = authWhitelistProperties.getWhitelist();
        this.tokenProperties = tokenProperties;
    }

    /**
     * JwtAuthenticationFilter 创建bean
     * @return JwtAuthenticationFilter的bean对象
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(){

        return new JwtAuthenticationFilter(
                this.jwtUtils,this.redisSecurityHandle,this.authWhitelist,this.tokenProperties);
    }

    /**
     * 将当前创建的bean排除在springboot的bean注册范围之内
     * @param jwtAuthenticationFilter
     * @return
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(JwtAuthenticationFilter jwtAuthenticationFilter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(jwtAuthenticationFilter);
        registration.setEnabled(false); // 关闭 Servlet 容器级自动注册，只保留 Security 链内的注册
        return registration;
    }

}
