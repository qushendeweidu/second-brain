package com.laodeng.backend.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.laodeng.backend.domain.po.UserRole;
import com.laodeng.backend.mapper.UserRoleMapper;
import com.laodeng.backend.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/7/29 17:00
 * @description 用户权限业务层实现类
 */
@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements UserRoleService {


}
