package com.laodeng.backend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.laodeng.backend.domain.dto.UserRoleUpdateDTO;
import com.laodeng.backend.domain.po.UserRole;
import com.laodeng.backend.domain.vo.UserRoleVO;

import java.util.List;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/7/29 17:00
 * @description 用户权限业务层接口
 */

public interface UserRoleService extends IService<UserRole> {

    UserRoleVO getUserRoleByUserId(Long userId);

    List<UserRoleVO> listUserRoles();

    Long createUserRole(UserRoleUpdateDTO userRoleUpdateDTO);

    boolean updateUserRole(UserRoleUpdateDTO userRoleUpdateDTO);

    void deleteUserRole(Long userId);
}
