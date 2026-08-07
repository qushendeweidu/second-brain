package com.laodeng.backend.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private Long userId;
    //被封印的结束时间
    private LocalDateTime  blockedTime;
    //是否永久封禁
    private Boolean blocked;
}
