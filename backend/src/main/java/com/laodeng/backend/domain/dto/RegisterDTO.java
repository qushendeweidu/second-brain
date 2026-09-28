package com.laodeng.backend.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterDTO {
    // 用户名
    @NotNull
    private String username;
    // 密码
    @NotNull
    private String password;
}
