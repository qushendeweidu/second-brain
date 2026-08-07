package com.laodeng.backend.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 用户角色与权限更新请求。
 */
@Data
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleUpdateDTO {

    @NotNull
    private Long userId;

    private List<String> roles;

    private List<String> permissions;
}
