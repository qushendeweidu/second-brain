package com.laodeng.backend.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/7/28 11:59
 * @description
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginDTO implements Serializable {
    // 用户名
    @NotNull
    private String username;
    // 密码
    @NotNull
    private String password;
}
