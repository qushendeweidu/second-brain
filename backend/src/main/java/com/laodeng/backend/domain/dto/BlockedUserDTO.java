package com.laodeng.backend.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.TimeUnit;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/8/7 22:40
 * @description
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlockedUserDTO {
    //用户id
    @NotNull
    private Long userId;
    // 封禁数字(具体时间单位由时间类型决定)
    @NotNull
    private Long blockedTime;
    // 过期时间类型
    @NotNull
    private TimeUnit timeUnit;
    //是否永久封禁
    @NotNull
    private Boolean blocked;
}
