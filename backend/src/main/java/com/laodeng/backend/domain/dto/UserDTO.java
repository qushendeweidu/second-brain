package com.laodeng.backend.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/7/26 12:04
 * @description 用户查询条件DTO类
 */

@Data
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private String username;

    private String nickName;

    private String email;

    private String phone;

    private Integer status;

    private List<String> roles;

    private List<String> permissions;

    private LocalDateTime userCreateStartTime;

    private LocalDateTime userCreateEndTime;

    private LocalDateTime userUpdateStartTime;

    private LocalDateTime userUpdateEndTime;

    private LocalDateTime roleCreateStartTime;

    private LocalDateTime roleCreateEndTime;

    private LocalDateTime roleUpdateStartTime;

    private LocalDateTime roleUpdateEndTime;
}
