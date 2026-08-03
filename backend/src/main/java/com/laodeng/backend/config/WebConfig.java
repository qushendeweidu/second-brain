package com.laodeng.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Configuration;
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

}
