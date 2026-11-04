package com.laodeng.backend.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.laodeng.backend.common.ErrorCode;
import com.laodeng.backend.domain.dto.UserRestrictDTO;
import com.laodeng.backend.domain.po.UserRestrict;
import com.laodeng.backend.exception.ThrowUtils;
import com.laodeng.backend.handler.RedisDataHandle;
import com.laodeng.backend.handler.RedisSecurityHandle;
import com.laodeng.backend.mapper.UserRestrictMapper;
import com.laodeng.backend.service.UserRestrictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/9/16 21:26
 * @description 一段时间内的访问次数限流
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class UserRestrictServiceImpl extends ServiceImpl<UserRestrictMapper, UserRestrict> implements UserRestrictService {
    private final RedisSecurityHandle redisSecurityHandle;

    @Override
    public void restrictUserToken(UserRestrictDTO restrictDTO) {
        log.info("当前正在对这个用户的长时Token进行限流");
        UserRestrict userRestrict = this.getOne(new LambdaQueryWrapper<UserRestrict>()
                .eq(UserRestrict::getUserId, restrictDTO.getUserId()));
        if (ObjectUtil.isEmpty(userRestrict)){
            userRestrict= UserRestrict.builder()
                    .restrictRequest(restrictDTO.getRestrictRequest())
                    .windowSecond(restrictDTO.getWindowSecond())
                    .userId(restrictDTO.getUserId())
                    .timeUnit(restrictDTO.getTimeUnit())
                    .build();
            ThrowUtils.throwIf(!this.save(userRestrict),ErrorCode.OPERATION_ERROR);
        }

        // DTO 传值与库表不一致时，将最新配置同步回 user_restrict 表
        boolean needUpdate = false;
        LambdaUpdateWrapper<UserRestrict> updateWrapper = new LambdaUpdateWrapper<>();
        if (!ObjectUtil.equal(restrictDTO.getRestrictRequest(), userRestrict.getRestrictRequest())) {
            updateWrapper.set(UserRestrict::getRestrictRequest, restrictDTO.getRestrictRequest());
            needUpdate = true;
        }
        if (!ObjectUtil.equal(restrictDTO.getWindowSecond(), userRestrict.getWindowSecond())) {
            updateWrapper.set(UserRestrict::getWindowSecond, restrictDTO.getWindowSecond());
            needUpdate = true;
        }
        if (!ObjectUtil.equal(restrictDTO.getTimeUnit(),userRestrict.getTimeUnit())){
            updateWrapper.set(UserRestrict::getTimeUnit,restrictDTO.getTimeUnit());
            needUpdate = true;
        }
        if (needUpdate) {
            updateWrapper.eq(UserRestrict::getUserId, restrictDTO.getUserId());
            ThrowUtils.throwIf(!this.update(updateWrapper), ErrorCode.OPERATION_ERROR);
        }

        this.redisSecurityHandle.restrictUserToken(restrictDTO.getUserId().toString(), restrictDTO.getRestrictRequest(), restrictDTO.getWindowSecond(), restrictDTO.getTimeUnit());
    }

    @Override
    public UserRestrict getUserRestrict(Long userId) {
        UserRestrict userRestrict = this.getOne(new LambdaQueryWrapper<UserRestrict>()
                .eq(UserRestrict::getUserId, userId));
        ThrowUtils.throwIf(userRestrict == null, ErrorCode.NOT_FOUND_ERROR);
        return userRestrict;
    }

}
