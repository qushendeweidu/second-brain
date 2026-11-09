package com.laodeng.backend.controller.shop;

import com.laodeng.backend.common.PageResult;
import com.laodeng.backend.common.R;
import com.laodeng.backend.domain.dto.GoodsCreateDTO;
import com.laodeng.backend.domain.dto.GoodsQueryDTO;
import com.laodeng.backend.domain.dto.GoodsUpdateDTO;
import com.laodeng.backend.domain.vo.GoodsVO;
import com.laodeng.backend.service.GoodsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/9/17 00:12
 * @description 商品相关接口
 */

@RestController
@RequestMapping("/shop/goods")
@RequiredArgsConstructor
public class ShopController {

    private final GoodsService goodsService;

    /**
     * 创建商品，卖家由登录态自动填充。
     *
     * @param goodsCreateDTO 商品创建参数
     * @return 新商品 id
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/create")
    public R<Long> createGoods(@Valid @RequestBody GoodsCreateDTO goodsCreateDTO) {
        return R.success(this.goodsService.createGoods(goodsCreateDTO));
    }

    /**
     * 更新商品，仅卖家本人或 ADMIN 可操作。
     *
     * @param goodsUpdateDTO 商品更新参数
     * @return 是否更新成功
     */
    @PatchMapping("/update")
    public R<Boolean> updateGoods(@Valid @RequestBody GoodsUpdateDTO goodsUpdateDTO) {
        return R.success(this.goodsService.updateGoods(goodsUpdateDTO));
    }

    /**
     * 删除商品，仅卖家本人或 ADMIN 可操作。
     *
     * @param id 商品 id
     */
    @DeleteMapping("/{id}")
    public R<Void> deleteGoods(@PathVariable Long id) {
        this.goodsService.deleteGoods(id);
        return R.success();
    }

    /**
     * 按 id 查询商品详情。
     *
     * @param id 商品 id
     * @return 商品详情
     */
    @GetMapping("/{id}")
    public R<GoodsVO> getGoodsById(@PathVariable Long id) {
        return R.success(this.goodsService.getGoodsVOById(id));
    }

    /**
     * 分页查询商品。
     *
     * @param goodsQueryDTO 查询条件与分页参数
     * @return 分页结果
     */
    @PostMapping("/list")
    public R<PageResult<GoodsVO>> listGoods(@RequestBody GoodsQueryDTO goodsQueryDTO) {
        return R.success(this.goodsService.getGoodsVOByQuery(goodsQueryDTO));
    }

}
