package com.laodeng.backend.config;

import com.laodeng.backend.config.properties.FrontendProperties;
import com.laodeng.backend.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/7/28 11:52
 * @description security配置
 */

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {


    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final FrontendProperties frontendProperties;


    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {
        return http
                //开启cors配置
                .cors(Customizer.withDefaults())
                //关闭csrf配置
                .csrf(AbstractHttpConfigurer::disable)
                //JWT项目无状态
                .sessionManagement(
                        session ->
                                session.sessionCreationPolicy(
                                        SessionCreationPolicy.STATELESS
                                )
                )
                //请求权限规则
                .authorizeHttpRequests(
                        auth -> auth
                                //登录接口放行
                                .requestMatchers(
                                        "/user/login",
                                        "/user/register"
                                )
                                .permitAll() //放行上面的这些请求路径
                                //下面是包含所有的其他请求
                                .anyRequest()
                                //执行验证
                                .authenticated()
                )
                //添加JWT过滤器
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 前端地址
        config.setAllowedOrigins(List.of(frontendProperties.getUrl()));
        // 请求方法
        config.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "OPTIONS"
                )
        );
        // 请求头
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(frontendProperties.getMaxAge());
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
