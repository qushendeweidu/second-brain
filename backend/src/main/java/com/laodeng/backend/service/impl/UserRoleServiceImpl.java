package com.laodeng.backend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.laodeng.backend.common.ErrorCode;
import com.laodeng.backend.domain.dto.UserRoleUpdateDTO;
import com.laodeng.backend.domain.po.UserRole;
import com.laodeng.backend.domain.vo.UserRoleVO;
import com.laodeng.backend.exception.ThrowUtils;
import com.laodeng.backend.mapper.UserMapper;
import com.laodeng.backend.mapper.UserRoleMapper;
import com.laodeng.backend.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    private final UserMapper userMapper;

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
    public List<UserRoleVO> listUserRoles() {
        return this.list().stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUserRole(UserRoleUpdateDTO userRoleUpdateDTO) {
        ThrowUtils.throwIf(this.userMapper.selectById(userRoleUpdateDTO.getUserId()) == null,
                ErrorCode.USER_NOT_FOUND_ERROR);
        long count = this.count(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userRoleUpdateDTO.getUserId()));
        ThrowUtils.throwIf(count > 0, ErrorCode.OPERATION_ERROR, "用户角色已存在");

        UserRole userRole = new UserRole();
        BeanUtil.copyProperties(userRoleUpdateDTO, userRole);
        ThrowUtils.throwIf(!this.save(userRole), ErrorCode.OPERATION_ERROR);
        return userRole.getId();
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
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUserRole(Long userId) {
        boolean result = this.remove(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId));
        ThrowUtils.throwIf(!result, ErrorCode.NO_ROLE_ERROR);
    }

    private UserRoleVO toVO(UserRole userRole) {
        UserRoleVO userRoleVO = new UserRoleVO();
        BeanUtil.copyProperties(userRole, userRoleVO);
        return userRoleVO;
    }

}
