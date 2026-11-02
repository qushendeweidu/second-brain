package com.laodeng.backend.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.concurrent.TimeUnit;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/9/21 12:17
 * @description 修改用户限流配置DTO类
 */

@Data
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserRestrictDTO {
    /**
     * 关联用户id
     */
    private Long userId;
    /**
     * 限制请求次数
     */
    private Long restrictRequest;
    /**
     * 一段限制次数请求的刷新时间
     */
    private Long windowSecond;
    /**
     * 限制的时间单位
     */
    private TimeUnit timeUnit;

}
