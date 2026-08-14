package com.laodeng.backend.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/8/13 23:11
 * @description 分页条件DTO类
 */

@Data
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class PageDTO {

    private Long pageNum;   // 页码，默认 1

    private Long pageSize;  // 每页条数，默认 10

}
