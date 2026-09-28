package com.laodeng.backend.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.laodeng.backend.domain.po.UserRestrict;
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
    public void restrictUserToken(Long userId, TimeUnit timeUnit,Long restrictId) {
        log.info("当前正在对这个用户的长时Token进行限流");
        UserRestrict userRestrict = this.getById(restrictId);
        this.redisSecurityHandle.restrictUserToken(userId.toString(),userRestrict.getRestrictRequest(),userRestrict.getWindowSecond(),timeUnit);
    }

}
