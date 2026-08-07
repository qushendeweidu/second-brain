package com.laodeng.backend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.laodeng.backend.common.ErrorCode;
import com.laodeng.backend.domain.dto.LoginDTO;
import com.laodeng.backend.domain.dto.UserCreateDTO;
import com.laodeng.backend.domain.dto.UserDTO;
import com.laodeng.backend.domain.dto.UserUpdateDTO;
import com.laodeng.backend.domain.po.User;
import com.laodeng.backend.domain.po.UserProfile;
import com.laodeng.backend.domain.po.UserRole;
import com.laodeng.backend.domain.vo.UserVO;
import com.laodeng.backend.exception.ThrowUtils;
import com.laodeng.backend.handler.RedisSecurityHandle;
import com.laodeng.backend.mapper.UserMapper;
import com.laodeng.backend.service.UserProfileService;
import com.laodeng.backend.service.UserRoleService;
import com.laodeng.backend.service.UserService;
import com.laodeng.backend.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/7/26 09:16
 * @description 用户业务层实现类
 */

@Log4j2
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserRoleService userRoleService;
    private final UserProfileService userProfileService;
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final RedisSecurityHandle redisSecurityHandle;
    private final PasswordEncoder passwordEncoder;
    private static final String DEFAULT_ROLE = "USER";
    private static final String DEFAULT_PERMISSION = "user:read";

    /**
     * 登陆
     * @param loginDTO 登陆信息
     * @return 登陆结果
     */
    @Override
    public String login(LoginDTO loginDTO) {
        log.info("用户:{} 尝试登陆", loginDTO.getUsername());
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, loginDTO.getUsername());
        User user = this.getOne(queryWrapper);
        ThrowUtils.throwIf(ObjUtil.isEmpty( user), ErrorCode.NOT_FOUND_ERROR);
        log.info("用户:{} 登陆", user.getUsername());
        String token = null;
        if (this.passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            Long userId = user.getId();
            if (this.redisSecurityHandle.checkSecurityKey(userId.toString())){
                token = this.redisSecurityHandle.getSecurityKey(userId.toString());
            }else {
                token = this.jwtUtils.createToken(userId);
                this.redisSecurityHandle.createSecurityKey(userId.toString(), token);
            }
        }
        ThrowUtils.throwIf("0".equals(token), ErrorCode.USER_BLOCKED);
        return token;
    }

    @Override
    public UserVO getUserVOById(Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        User user = this.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.USER_NOT_FOUND_ERROR);

        UserRole userRole = this.userRoleService.getOne(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id)
        );
        UserProfile userProfile = this.userProfileService.getOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, id)
        );

        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        userVO.setUserCreateTime(user.getCreateTime());
        userVO.setUserUpdateTime(user.getUpdateTime());
        if (userRole != null) {
            userVO.setRoles(userRole.getRoles());
            userVO.setPermissions(userRole.getPermissions());
            userVO.setRoleCreateTime(userRole.getCreateTime());
            userVO.setRoleUpdateTime(userRole.getUpdateTime());
        }
        if (userProfile != null) {
            userVO.setAvatar(userProfile.getAvatar());
        }
        return userVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserCreateDTO userCreateDTO) {
        long count = this.count(
                new LambdaQueryWrapper<User>().eq(User::getUsername, userCreateDTO.getUsername())
        );
        ThrowUtils.throwIf(count > 0, ErrorCode.USER_NAME_REPEAT);

        User user = new User();
        BeanUtil.copyProperties(userCreateDTO, user);
        user.setPassword(this.passwordEncoder.encode(userCreateDTO.getPassword()));
        user.setStatus(userCreateDTO.getStatus() == null ? 1 : userCreateDTO.getStatus());
        ThrowUtils.throwIf(!this.save(user), ErrorCode.OPERATION_ERROR);

        ThrowUtils.throwIf(!this.userRoleService.save(UserRole.builder()
                .userId(user.getId())
                .roles(List.of(DEFAULT_ROLE))
                .permissions(List.of(DEFAULT_PERMISSION))
                .build()), ErrorCode.OPERATION_ERROR);
        ThrowUtils.throwIf(!this.userProfileService.save(UserProfile.builder()
                .userId(user.getId())
                .bio("这个人很懒，什么都没有留下")
                .build()), ErrorCode.OPERATION_ERROR);
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(UserUpdateDTO userUpdateDTO) {
        User user = this.getById(userUpdateDTO.getId());
        ThrowUtils.throwIf(user == null, ErrorCode.USER_NOT_FOUND_ERROR);

        if (StrUtil.isNotBlank(userUpdateDTO.getUsername())
                && !ObjUtil.equal(user.getUsername(), userUpdateDTO.getUsername())) {
            long count = this.count(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, userUpdateDTO.getUsername())
                    .ne(User::getId, userUpdateDTO.getId()));
            ThrowUtils.throwIf(count > 0, ErrorCode.USER_NAME_REPEAT);
            user.setUsername(userUpdateDTO.getUsername());
        }
        if (StrUtil.isNotBlank(userUpdateDTO.getPassword())) {
            user.setPassword(this.passwordEncoder.encode(userUpdateDTO.getPassword()));
        }
        if (userUpdateDTO.getNickName() != null) {
            user.setNickName(userUpdateDTO.getNickName());
        }
        if (userUpdateDTO.getEmail() != null) {
            user.setEmail(userUpdateDTO.getEmail());
        }
        if (userUpdateDTO.getPhone() != null) {
            user.setPhone(userUpdateDTO.getPhone());
        }
        if (userUpdateDTO.getStatus() != null) {
            user.setStatus(userUpdateDTO.getStatus());
        }
        boolean result = this.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.USER_UPDATE_ERROR);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        User user = this.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.USER_NOT_FOUND_ERROR);
        this.userRoleService.remove(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id)
        );
        this.userProfileService.remove(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, id)
        );
        ThrowUtils.throwIf(!this.removeById(id), ErrorCode.OPERATION_ERROR);
        this.redisSecurityHandle.deleteSecurityKey(id.toString());
    }

    /**
     * 注册账户
     * @param loginDTO 登陆DTO层
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(LoginDTO loginDTO) {
        this.createUser(UserCreateDTO.builder()
                .username(loginDTO.getUsername())
                .password(loginDTO.getPassword())
                .build());
    }

    /**
     * 根据用户DTO获取用户VO
     * @param userDTO 用户DTO
     * @return 用户VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<UserVO> getUserVOByUserDTO(UserDTO userDTO) {
        log.info("当前的请求体:{}", userDTO);
        return this.userMapper.getUserVOByUserDTO(userDTO);
    }



}
