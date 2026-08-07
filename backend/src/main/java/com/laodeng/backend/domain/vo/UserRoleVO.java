package com.laodeng.backend.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户角色与权限响应。
 */
@Data
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleVO {

    private Long id;
    private Long userId;
    private List<String> roles;
    private List<String> permissions;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
