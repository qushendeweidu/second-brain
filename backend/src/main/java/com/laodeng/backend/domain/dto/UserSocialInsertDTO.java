package com.laodeng.backend.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/10/31 22:13
 * @description 用户身份类插入DTO类
 */

@Data
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserSocialInsertDTO {

    private Long userId;

    private String provider;

    private String providerUid;

    private String providerLogin;
}
