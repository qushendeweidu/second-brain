package com.laodeng.backend.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/8/5 23:44
 * @description 用户配置文件VO类
 */

@Data
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileVO {
    
    private Long userId;
    private String avatar;
    private String bio;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}
