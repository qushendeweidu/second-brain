package com.laodeng.backend.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
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
 * @date 2026/7/26 09:41
 * @description 用户权限实体类
 */

@Data
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "user_role", autoResultMap = true)
public class UserRole {
    @TableId
    private Long id;
    @TableField(value = "user_id")
    private Long userId;
    @TableField(value = "roles", typeHandler = JacksonTypeHandler.class)
    private List<String> roles;
    @TableField(value = "permissions", typeHandler = JacksonTypeHandler.class)
    private List<String> permissions;

    @TableField(value = "create_time")
    private LocalDateTime createTime;

    @TableField(value = "update_time")
    private LocalDateTime updateTime;

}
