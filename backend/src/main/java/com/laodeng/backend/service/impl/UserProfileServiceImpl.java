package com.laodeng.backend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.laodeng.backend.common.ErrorCode;
import com.laodeng.backend.domain.dto.UserProfileCreateDTO;
import com.laodeng.backend.domain.dto.UserProfileUpdateDTO;
import com.laodeng.backend.domain.po.UserProfile;
import com.laodeng.backend.domain.vo.UserProfileVO;
import com.laodeng.backend.exception.ThrowUtils;
import com.laodeng.backend.mapper.UserMapper;
import com.laodeng.backend.mapper.UserProfileMapper;
import com.laodeng.backend.service.UserProfileService;
import com.laodeng.backend.utils.JwtUtils;
import com.laodeng.backend.utils.MinIOUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/8/6 12:37
 * @description 用户配置文件业务层实现类
 */
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl extends ServiceImpl<UserProfileMapper, UserProfile> implements UserProfileService {
    private final MinIOUtils minIOUtils;
    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;

    @Override
    public String saveUserAvatar(MultipartFile multipartFile, Long userId, HttpServletRequest request) {
        checkManagePermission(userId, request);
        UserProfile userProfile = getProfile(userId);
        String avatarUrl = this.minIOUtils.uploadFile(multipartFile);
        userProfile.setAvatar(avatarUrl);
        ThrowUtils.throwIf(!this.updateById(userProfile), ErrorCode.USER_UPDATE_ERROR);
        return avatarUrl;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUserProfile(UserProfileUpdateDTO userProfileUpdateDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(userProfileUpdateDTO.getUserId() == null, ErrorCode.PARAMS_ERROR);
        checkManagePermission(userProfileUpdateDTO.getUserId(), request);
        UserProfile userProfile = getProfile(userProfileUpdateDTO.getUserId());
        if (userProfileUpdateDTO.getBio() != null) {
            userProfile.setBio(userProfileUpdateDTO.getBio());
        }
        if (userProfileUpdateDTO.getAvatar() != null) {
            userProfile.setAvatar(userProfileUpdateDTO.getAvatar());
        }
        ThrowUtils.throwIf(!this.updateById(userProfile), ErrorCode.USER_UPDATE_ERROR);
        return true;
    }


    @Override
    public UserProfileVO getUserProfileByUserId(Long userId) {
        ThrowUtils.throwIf(userId == null || ObjUtil.isEmpty(userId), ErrorCode.PARAMS_ERROR);
        return toVO(getProfile(userId));
    }

    @Override
    public UserProfileVO getUserProfileBySelf(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        ThrowUtils.throwIf(token == null || token.isEmpty(), ErrorCode.TOKEN_ERROR);
        Long userId = this.jwtUtils.extractId(token);
        return toVO(getProfile(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUserProfile(UserProfileCreateDTO userProfileCreateDTO, HttpServletRequest request) {
        Long userId = userProfileCreateDTO.getUserId();
        checkManagePermission(userId, request);
        ThrowUtils.throwIf(this.userMapper.selectById(userId) == null, ErrorCode.USER_NOT_FOUND_ERROR);
        long count = this.count(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId));
        ThrowUtils.throwIf(count > 0, ErrorCode.OPERATION_ERROR, "用户资料已存在");

        UserProfile userProfile = new UserProfile();
        BeanUtil.copyProperties(userProfileCreateDTO, userProfile);
        ThrowUtils.throwIf(!this.save(userProfile), ErrorCode.OPERATION_ERROR);
        return userProfile.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUserProfile(Long userId, HttpServletRequest request) {
        checkManagePermission(userId, request);
        boolean result = this.remove(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId));
        ThrowUtils.throwIf(!result, ErrorCode.NOT_FOUND_ERROR);
    }

    /**
     * 获取用户配置文件类
     * @param userId
     * @return
     */
    private UserProfile getProfile(Long userId) {
        UserProfile userProfile = this.getOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId));
        ThrowUtils.throwIf(userProfile == null, ErrorCode.NOT_FOUND_ERROR);
        return userProfile;
    }

    /**
     * 将用户配置文件转化成VO类
     * @param userProfile
     * @return
     */
    private UserProfileVO toVO(UserProfile userProfile) {
        UserProfileVO userProfileVO = new UserProfileVO();
        BeanUtil.copyProperties(userProfile, userProfileVO);
        return userProfileVO;
    }

    /**
     * 判断当前是本人或者是管理员
     * @param userId
     * @param request
     */
    private void checkManagePermission(Long userId, HttpServletRequest request) {
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR);
        String token = request.getHeader("Authorization");
        ThrowUtils.throwIf(token == null || token.isEmpty(), ErrorCode.TOKEN_ERROR);
        Long currentUserId = this.jwtUtils.extractId(token);
        ThrowUtils.throwIf(!ObjUtil.equal(userId, currentUserId) && !isAdmin(token),
                ErrorCode.NO_AUTH_ERROR);
    }

    /**
     * 只用来判断当前是不是管理员角色
     * @param request
     */
    private void checkAdminPermission(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        ThrowUtils.throwIf(token == null || token.isEmpty(), ErrorCode.TOKEN_ERROR);
        ThrowUtils.throwIf(!isAdmin(token), ErrorCode.NO_AUTH_ERROR);
    }

    /**
     * 确认当前token是否为admin权限
     * @param token
     * @return
     */
    private boolean isAdmin(String token) {
        List<String> roles = this.jwtUtils.extractRoles(token);
        return roles != null && roles.contains("ADMIN");
    }

}
