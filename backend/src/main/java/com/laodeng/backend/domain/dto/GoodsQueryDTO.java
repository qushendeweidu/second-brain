package com.laodeng.backend.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/11/08
 * @description 商品分页查询请求参数
 */

@Data
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class GoodsQueryDTO {

    /** 商品名称关键字，模糊匹配 */
    private String name;

    /** 卖家用户 id，精确匹配 */
    private Long sellerUserId;

    private PageDTO pageDTO;

}
