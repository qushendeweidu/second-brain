package com.laodeng.backend.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/7/26 10:33
 * @description
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class UserVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String username;

    private String nickName;

    private String email;

    private String phone;

    private Integer status;

    private List<String> roles;

    private List<String> permissions;

    private LocalDateTime userCreateTime;

    private LocalDateTime userUpdateTime;

    private LocalDateTime roleCreateTime;

    private LocalDateTime roleUpdateTime;
}
