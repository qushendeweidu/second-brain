package com.laodeng.backend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.laodeng.backend.domain.dto.UserRestrictDTO;
import com.laodeng.backend.domain.po.UserRestrict;

import java.util.concurrent.TimeUnit;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/9/16 21:27
 * @description 用户请求数量限流
 */
public interface UserRestrictService extends IService<UserRestrict> {
    void restrictUserToken(UserRestrictDTO restrictDTO);

    /**
     * 查询用户限流配置
     *
     * @param userId 用户id
     * @return 限流配置
     */
    UserRestrict getUserRestrict(Long userId);
}
