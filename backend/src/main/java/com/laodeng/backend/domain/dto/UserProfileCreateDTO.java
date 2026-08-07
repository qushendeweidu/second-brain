package com.laodeng.backend.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 用户资料创建请求。
 */
@Data
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileCreateDTO {

    @NotNull
    private Long userId;

    @Size(max = 255)
    private String avatar;

    @Size(max = 255)
    private String bio;
}
