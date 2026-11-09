package com.laodeng.backend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.laodeng.backend.common.PageResult;
import com.laodeng.backend.domain.dto.GoodsCreateDTO;
import com.laodeng.backend.domain.dto.GoodsQueryDTO;
import com.laodeng.backend.domain.dto.GoodsUpdateDTO;
import com.laodeng.backend.domain.po.Goods;
import com.laodeng.backend.domain.vo.GoodsVO;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/11/08
 * @description 商品业务层接口
 */

public interface GoodsService extends IService<Goods> {

    Long createGoods(GoodsCreateDTO goodsCreateDTO);

    boolean updateGoods(GoodsUpdateDTO goodsUpdateDTO);

    void deleteGoods(Long id);

    GoodsVO getGoodsVOById(Long id);

    PageResult<GoodsVO> getGoodsVOByQuery(GoodsQueryDTO goodsQueryDTO);

}
