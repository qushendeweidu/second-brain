package com.laodeng.backend.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 用户创建请求。
 */
@Data
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateDTO {

    @NotBlank
    @Size(max = 255)
    private String username;

    @NotBlank
    @Size(min = 6, max = 255)
    private String password;

    @Size(max = 255)
    private String nickName;

    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 255)
    private String phone;

    private Integer status;
}
