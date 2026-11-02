package com.laodeng.backend.annotation;

import java.lang.annotation.*;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/11/2 15:43
 * @description 接口限流注解。
 * 标注在需要按「用户维度」限流的 Controller 方法上；
 * 档位（请求次数 / 窗口）取自 user_restrict 表该用户的配置，未配置则不限流。
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
}
