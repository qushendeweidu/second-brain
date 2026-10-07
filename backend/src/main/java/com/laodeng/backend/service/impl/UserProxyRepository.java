package com.laodeng.backend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.laodeng.backend.common.ErrorCode;
import com.laodeng.backend.domain.dto.UserCreateDTO;
import com.laodeng.backend.domain.po.User;
import com.laodeng.backend.domain.po.UserProfile;
import com.laodeng.backend.domain.po.UserRole;
import com.laodeng.backend.exception.BusinessException;
import com.laodeng.backend.exception.ThrowUtils;
import com.laodeng.backend.mapper.UserMapper;
import com.laodeng.backend.mapper.UserProfileMapper;
import com.laodeng.backend.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/9/17 00:06
 * @description 用户数据操作代理类
 */

@Log4j2
@Repository
@RequiredArgsConstructor
public class UserProxyRepository {


    private final PasswordEncoder passwordEncoder;
    private static final String DEFAULT_ROLE = "USER";
    private static final String DEFAULT_PERMISSION = "user:read";
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserRoleMapper userRoleMapper;


    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserCreateDTO userCreateDTO) {
        User user = new User();
        try {

            BeanUtil.copyProperties(userCreateDTO, user);
            user.setPassword(this.passwordEncoder.encode(userCreateDTO.getPassword()));
            user.setStatus(userCreateDTO.getStatus() == null ? 1 : userCreateDTO.getStatus());
            this.userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.USER_NAME_REPEATED_ERROR);
        }
        // 向数据库插入用户权限数据若失败则抛出异常
        ThrowUtils.throwIf(this.userRoleMapper.insert(UserRole.builder()
                .userId(user.getId())
                .roles(List.of(DEFAULT_ROLE))
                .permissions(List.of(DEFAULT_PERMISSION))
                .build()) <= 0, ErrorCode.USER_ROLE_CREATED_ERROR);
        // 向数据库插入用户配置文件数据如果失败则抛异常
        ThrowUtils.throwIf(this.userProfileMapper.insert(UserProfile.builder()
                .userId(user.getId())
                .bio("这个人很懒，什么都没有留下")
                .build()) <= 0, ErrorCode.USER_PROFILE_CREATED_ERROR);
        return user.getId();
    }
}
