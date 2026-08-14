package com.laodeng.backend.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/8/14 10:30
 * @description 冲洗分页Page类
 * 只给动态SQL.xml文件使用(原因是MP底层bug)
 */

public class CustomPage<T> extends Page<T> {
    public CustomPage(long current, long size) {
        super(current, size);
    }

    @Override
    public boolean optimizeCountSql() {
        return false;
    }
}
