package com.laodeng.backend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.laodeng.backend.common.ErrorCode;
import com.laodeng.backend.domain.dto.UserRoleUpdateDTO;
import com.laodeng.backend.domain.po.UserRole;
import com.laodeng.backend.domain.vo.UserRoleVO;
import com.laodeng.backend.exception.ThrowUtils;
import com.laodeng.backend.handler.RedisSecurityHandle;
import com.laodeng.backend.mapper.UserRoleMapper;
import com.laodeng.backend.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/7/29 17:00
 * @description 用户权限业务层实现类
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements UserRoleService {

    private final RedisSecurityHandle redisSecurityHandle;

    @Override
    public UserRoleVO getUserRoleByUserId(Long userId) {
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR);
        UserRole userRole = this.getOne(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)
        );
        ThrowUtils.throwIf(userRole == null, ErrorCode.NO_ROLE_ERROR);
        return toVO(userRole);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUserRole(UserRoleUpdateDTO userRoleUpdateDTO) {
        UserRole userRole = this.getOne(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userRoleUpdateDTO.getUserId()));
        ThrowUtils.throwIf(userRole == null, ErrorCode.NO_ROLE_ERROR);
        if (userRoleUpdateDTO.getRoles() != null) {
            userRole.setRoles(userRoleUpdateDTO.getRoles());
        }
        if (userRoleUpdateDTO.getPermissions() != null) {
            userRole.setPermissions(userRoleUpdateDTO.getPermissions());
        }
        ThrowUtils.throwIf(!this.updateById(userRole), ErrorCode.OPERATION_ERROR);

        //当前用户更新成功，目前需要清除redis中存储的token从而使过滤器拦截报错从而促使用户重新尝试登陆刷新用户的权限存储在token的数据
        this.redisSecurityHandle.deleteSecurityKey(userRoleUpdateDTO.getUserId().toString());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUserRole(Long userId) {
        boolean result = this.remove(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId));
        ThrowUtils.throwIf(!result, ErrorCode.NO_ROLE_ERROR);
        //当前用户更新成功，目前需要清除redis中存储的token从而使过滤器拦截报错从而促使用户重新尝试登陆刷新用户的权限存储在token的数据
        this.redisSecurityHandle.deleteSecurityKey(userId.toString());
    }

    private UserRoleVO toVO(UserRole userRole) {
        UserRoleVO userRoleVO = new UserRoleVO();
        BeanUtil.copyProperties(userRole, userRoleVO);
        return userRoleVO;
    }

}
