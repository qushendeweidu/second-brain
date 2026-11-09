package com.laodeng.backend.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/11/08
 * @description 商品VO类
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class GoodsVO {

    private Long id;

    private String name;

    private BigDecimal price;

    private String image;

    private Long sellerUserId;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
