package com.laodeng.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laodeng.backend.domain.po.Goods;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/11/08
 * @description 商品数据访问层
 */

@Mapper
public interface GoodsMapper extends BaseMapper<Goods> {
}
