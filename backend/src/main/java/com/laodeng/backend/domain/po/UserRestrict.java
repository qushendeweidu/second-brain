package com.laodeng.backend.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/9/17 00:06
 * @description 用户限流实体类
 */

@Data
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "user_restrict")
public class UserRestrict {

    /**
     * 主键id
     */
    private Long id;

    /**
     * 限制请求次数
     */
    private Long restrictRequest;
    /**
     * 一段限制次数请求的刷新时间
     */
    private Long windowSecond;

}
