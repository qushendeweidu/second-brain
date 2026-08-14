package com.laodeng.backend.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/8/13 21:47
 * @description 分页结果类
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private List<T> records; // 当前页数据
    private long total; // 全部数据数量
    private long pageNum; // 当前页码
    private long pageSize; // 每页条数
    private long pages; // 总页数


    public static <T> PageResult<T> of(List<T> records, long total, long pageNum, long pageSize)
    {
        return PageResult.<T>builder()
                .records(records)
                .total(total)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .pages(pageSize == 0 ? 0 : (total + pageSize - 1) / pageSize) // 因为java整数除法会直接忽略小数
                .build();
    }
}
