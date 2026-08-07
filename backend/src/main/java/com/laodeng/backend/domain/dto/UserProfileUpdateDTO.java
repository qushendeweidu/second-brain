package com.laodeng.backend.domain.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/8/6 23:41
 * @description 用户配置文件更新DTO类
 */
@Data
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateDTO {

    // 用户id
    private Long userId;
    //用户头像
    private String avatar;
    // 用户简介
    private String bio;

}
