package com.laodeng.backend.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/8/5 22:53
 * @description 用户配置文件查询DTO类
 */

@Data
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileSelectDTO {
    private String userId;
    private String avatar;
    private String bio;
    private String createStartTime;
    private String createEndTime;
    private String updateStartTime;
    private String updateEndTime;

}
