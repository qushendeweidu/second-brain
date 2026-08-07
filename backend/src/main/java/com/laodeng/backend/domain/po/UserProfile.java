package com.laodeng.backend.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/8/5 23:01
 * @description 用户配置文件类
 */

@Data
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "user_profile", autoResultMap = true)
public class UserProfile {
    @TableId
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("avatar")
    private String avatar;
    @TableField("bio")
    private String bio;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;

}
