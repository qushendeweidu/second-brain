package com.laodeng.backend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.laodeng.backend.common.ErrorCode;
import com.laodeng.backend.config.properties.TokenProperties;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private final TokenProperties tokenProperties;
    private final AntPathMatcher rolematcher = new AntPathMatcher();

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
        String token = request.getHeader(tokenProperties.getGeneration());
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
        ThrowUtils.throwIf(ObjectUtil.isEmpty(request), ErrorCode.PARAMS_ERROR); //参数请求对象为空则抛异常
        Authentication auth = SecurityContextHolder.getContext().getAuthentication(); // 获取security封装对象
        ThrowUtils.throwIf(auth == null || !auth.isAuthenticated(), ErrorCode.NOT_LOGIN_ERROR);//判断当前拿到的security对象是否存在
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities(); // 这里是只读场景使用extend
        ThrowUtils.throwIf(ObjectUtil.isEmpty(authorities), ErrorCode.NO_AUTH_ERROR); // 当前存储的security的用户ID为空则抛异常
        List<String> roles = authorities.stream()
                .map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .toList();
        Long currentUserId = (Long) auth.getPrincipal(); // 获取当前请求的security上下文的用户ID（这里的userId就是在拦截器处由携带的token存入的）
        // 判断当前用户的security上下文中是否包含ADMIN权限
        ThrowUtils.throwIf(!ObjectUtil.equal(userId, currentUserId) && !roles.contains("ADMIN"), ErrorCode.NO_AUTH_ERROR);
    }

}
