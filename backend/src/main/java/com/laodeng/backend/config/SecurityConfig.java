package com.laodeng.backend.config;

import com.laodeng.backend.config.properties.AuthWhitelistProperties;
import com.laodeng.backend.config.properties.FrontendProperties;
import com.laodeng.backend.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
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
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final FrontendProperties frontendProperties;
    private final AuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final AuthWhitelistProperties authWhitelistProperties;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          FrontendProperties frontendProperties,
                          AuthenticationEntryPoint jwtAuthenticationEntryPoint,
                          AuthWhitelistProperties authWhitelistProperties) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.frontendProperties = frontendProperties;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.authWhitelistProperties = authWhitelistProperties;
    }

    /**
     * 权限拦截器
     * @param http http请求
     * @return 响应一个拦截器链条
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
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
                                       this.authWhitelistProperties.getWhitelist().toArray(String[]::new)
                                )
                                .permitAll() //放行上面的这些请求路径
                                //下面是包含所有的其他请求
                                .anyRequest()
                                //执行验证
                                .authenticated()
                )
                // ===== 关键：注册认证失败处理器 =====
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(this.jwtAuthenticationEntryPoint)
                )
                //添加JWT过滤器
                .addFilterBefore(
                        this.jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许跨域的前端地址
        config.setAllowedOrigins(List.of(this.frontendProperties.getUrl()));
        // 允许请求的请求方法
        config.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );
        // 允许请求的请求头请求头
        config.setAllowedHeaders(List.of("*"));
        // 统一配置暴露哪些响应头给前端，前端 Axios 就能无缝读取了
        config.setExposedHeaders(List.of("Authorization","RefreshToken"));
        config.setMaxAge(this.frontendProperties.getMaxAge()); // 缓存预检请求响应的时间（以秒为单位）
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // 对于所有路径实现上面的配置
        return source;
    }
}
