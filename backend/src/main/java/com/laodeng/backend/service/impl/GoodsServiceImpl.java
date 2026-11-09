package com.laodeng.backend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.laodeng.backend.common.ErrorCode;
import com.laodeng.backend.common.PageResult;
import com.laodeng.backend.domain.dto.GoodsCreateDTO;
import com.laodeng.backend.domain.dto.GoodsQueryDTO;
import com.laodeng.backend.domain.dto.GoodsUpdateDTO;
import com.laodeng.backend.domain.dto.PageDTO;
import com.laodeng.backend.domain.po.Goods;
import com.laodeng.backend.domain.vo.GoodsVO;
import com.laodeng.backend.exception.ThrowUtils;
import com.laodeng.backend.mapper.GoodsMapper;
import com.laodeng.backend.service.GoodsService;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/11/08
 * @description 商品业务层实现类
 */

@Log4j2
@Service
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements GoodsService {

    /**
     * 创建商品：卖家从当前登录态获取，忽略请求体中的卖家字段，防止伪造归属。
     *
     * @param goodsCreateDTO 商品创建参数
     * @return 新商品 id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createGoods(GoodsCreateDTO goodsCreateDTO) {
        Goods goods = new Goods();
        BeanUtil.copyProperties(goodsCreateDTO, goods);
        goods.setSellerUserId(getCurrentUserId());
        boolean saved = this.save(goods);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR);
        return goods.getId();
    }

    /**
     * 更新商品：仅更新非空字段；仅卖家本人或 ADMIN 可操作。
     *
     * @param goodsUpdateDTO 商品更新参数
     * @return 是否更新成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateGoods(GoodsUpdateDTO goodsUpdateDTO) {
        Goods existing = getExistingGoods(goodsUpdateDTO.getId());
        checkOwner(existing);
        ThrowUtils.throwIf(
                ObjectUtil.isEmpty(goodsUpdateDTO.getName())
                        && ObjectUtil.isEmpty(goodsUpdateDTO.getPrice())
                        && ObjectUtil.isEmpty(goodsUpdateDTO.getImage())
                        && ObjectUtil.isEmpty(goodsUpdateDTO.getDescription()),
                ErrorCode.PARAMS_EMPTY_ERROR);
        Goods update = new Goods();
        BeanUtil.copyProperties(goodsUpdateDTO, update);
        boolean updated = this.updateById(update);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR);
        return true;
    }

    /**
     * 删除商品：仅卖家本人或 ADMIN 可操作。
     *
     * @param id 商品 id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGoods(Long id) {
        Goods existing = getExistingGoods(id);
        checkOwner(existing);
        boolean removed = this.removeById(id);
        ThrowUtils.throwIf(!removed, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 按 id 查询商品详情。
     *
     * @param id 商品 id
     * @return 商品 VO
     */
    @Override
    public GoodsVO getGoodsVOById(Long id) {
        Goods goods = getExistingGoods(id);
        return BeanUtil.copyProperties(goods, GoodsVO.class);
    }

    /**
     * 分页查询商品：支持名称模糊匹配与卖家精确筛选，按创建时间倒序。
     *
     * @param goodsQueryDTO 查询条件与分页参数
     * @return 分页结果
     */
    @Override
    public PageResult<GoodsVO> getGoodsVOByQuery(GoodsQueryDTO goodsQueryDTO) {
        PageDTO pageDTO = goodsQueryDTO.getPageDTO();
        long pageNum = ObjectUtil.isEmpty(pageDTO)
                || ObjectUtil.isEmpty(pageDTO.getPageNum())
                || pageDTO.getPageNum() < 1
                || pageDTO.getPageNum() > 100
                ? 1 : pageDTO.getPageNum();
        long pageSize = ObjectUtil.isEmpty(pageDTO)
                || ObjectUtil.isEmpty(pageDTO.getPageSize())
                || pageDTO.getPageSize() < 1
                || pageDTO.getPageSize() > 100
                ? 10 : pageDTO.getPageSize();

        LambdaQueryWrapper<Goods> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(ObjectUtil.isNotEmpty(goodsQueryDTO.getName()), Goods::getName, goodsQueryDTO.getName())
                .eq(ObjectUtil.isNotEmpty(goodsQueryDTO.getSellerUserId()), Goods::getSellerUserId, goodsQueryDTO.getSellerUserId())
                .orderByDesc(Goods::getCreateTime);

        Page<Goods> page = this.page(new Page<>(pageNum, pageSize), queryWrapper);
        List<GoodsVO> records = BeanUtil.copyToList(page.getRecords(), GoodsVO.class);
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 获取存在的商品，不存在则抛异常。
     *
     * @param id 商品 id
     * @return 商品实体
     */
    private Goods getExistingGoods(Long id) {
        ThrowUtils.throwIf(ObjectUtil.isEmpty(id), ErrorCode.PARAMS_EMPTY_ERROR);
        Goods goods = this.getById(id);
        ThrowUtils.throwIf(ObjectUtil.isEmpty(goods), ErrorCode.NOT_FOUND_ERROR);
        return goods;
    }

    /**
     * 校验当前登录用户是否为商品卖家本人或 ADMIN。
     *
     * @param goods 商品实体
     */
    private void checkOwner(Goods goods) {
        Long currentUserId = getCurrentUserId();
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> ObjectUtil.equal("ROLE_ADMIN", authority));
        ThrowUtils.throwIf(
                ObjectUtil.notEqual(currentUserId, goods.getSellerUserId()) && !isAdmin,
                ErrorCode.NO_AUTH_ERROR);
    }

    /**
     * 从 Spring Security 上下文获取当前登录用户 id（由 JWT 过滤器写入 principal）。
     *
     * @return 当前用户 id
     */
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        ThrowUtils.throwIf(auth == null || !(auth.getPrincipal() instanceof Long), ErrorCode.NOT_LOGIN_ERROR);
        return (Long) auth.getPrincipal();
    }

}
