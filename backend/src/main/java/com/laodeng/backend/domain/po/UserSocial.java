package com.laodeng.backend.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/10/22 20:50
 * @description
 */

@Data
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "user_social", autoResultMap = true)
public class UserSocial {

    private Long id;

    private Long userId;

    private String provider;

    private String providerUid;

    private String providerLogin;

    private LocalDateTime createdTime;
}
