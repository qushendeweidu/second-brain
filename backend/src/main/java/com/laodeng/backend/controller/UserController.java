package com.laodeng.backend.controller;

import com.laodeng.backend.common.ErrorCode;
import com.laodeng.backend.common.PageResult;
import com.laodeng.backend.common.R;
import com.laodeng.backend.domain.dto.*;
import com.laodeng.backend.domain.vo.UserProfileVO;
import com.laodeng.backend.domain.vo.UserRoleVO;
import com.laodeng.backend.domain.vo.UserVO;
import com.laodeng.backend.exception.ThrowUtils;
import com.laodeng.backend.service.UserProfileService;
import com.laodeng.backend.service.UserRoleService;
import com.laodeng.backend.service.UserService;
import com.laodeng.backend.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/7/26 19:06
 * @description user 相关的接口
 */

@Log4j2
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Validated
public class UserController {
    private final UserService userService;
    private final UserRoleService userRoleService;
    private final UserProfileService  userProfileService;
    private final JwtUtils jwtUtils;

    // ==================== 登陆注册部分 ====================
    /**
     * 登陆接口
     * @param loginDTO
     * @return
     */
    @PostMapping("/login")
    public R<String> login(@RequestBody @Validated LoginDTO loginDTO) {
        String isLogin = this.userService.login(loginDTO);
        ThrowUtils.throwIf(isLogin==null, ErrorCode.PASSWORD_ERROR);
        return R.success(isLogin);
    }

    /**
     * 注册接口
     * @param loginDTO
     */
    @PostMapping("/register")
    public R<Void> register(@RequestBody @Validated LoginDTO loginDTO) {
        this.userService.register(loginDTO);
        return R.success();
    }

    // ==================== 用户部分 ====================

    /**
     * 管理员创建用户
     * @param userCreateDTO
     * @return
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public R<Long> createUser(@Valid @RequestBody UserCreateDTO userCreateDTO) {
        return R.success(this.userService.createUser(userCreateDTO));
    }


    /**
     * 删除用户
     * @param id
     * @return
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public R<Void> deleteUser(@PathVariable Long id) {
        this.userService.deleteUser(id);
        return R.success();
    }

    /**
     * 更新用户
     * @param userUpdateDTO
     * @return
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/update")
    public R<Boolean> updateUser(@Valid @RequestBody UserUpdateDTO userUpdateDTO) {
        return R.success(this.userService.updateUser(userUpdateDTO));
    }

    /**
     * 查询用户数据
     * @param userDTO
     * @return
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/list")
    public R<PageResult<UserVO>> getUserVOByUserDTO(@RequestBody UserDTO userDTO) {
        PageResult<UserVO> userVOByUserDTO = this.userService.getUserVOByUserDTO(userDTO);
        return R.success(userVOByUserDTO);
    }

    /**
     * 根据id查询
     * @param id
     * @return
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public R<UserVO> getUserById(@PathVariable Long id) {
        return R.success(this.userService.getUserVOById(id));
    }
    // ==================== 用户权限部分 ====================

    /**
     * 创建用户权限
     * @param userRoleUpdateDTO
     * @return
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/role")
    public R<Long> createUserRole(@Valid @RequestBody UserRoleUpdateDTO userRoleUpdateDTO) {
        return R.success(this.userRoleService.createUserRole(userRoleUpdateDTO));
    }

    /**
     * 删除用户权限
     * @param userId
     * @return
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/role/{userId}")
    public R<Void> deleteUserRole(@PathVariable Long userId) {
        this.userRoleService.deleteUserRole(userId);
        return R.success();
    }

    /**
     * 更新用户权限
     * @param userRoleUpdateDTO
     * @return
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/role")
    public R<Boolean> updateUserRole(@Valid @RequestBody UserRoleUpdateDTO userRoleUpdateDTO) {
        return R.success(this.userRoleService.updateUserRole(userRoleUpdateDTO));
    }


    /**
     * 获取权限
     * @param userId
     * @return
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/role/{userId}")
    public R<UserRoleVO> getUserRole(@PathVariable Long userId) {
        return R.success(this.userRoleService.getUserRoleByUserId(userId));
    }

    // ==================== 用户配置文件部分 ====================

    /**
     * 创建用户配置文件类
     * @param userProfileCreateDTO
     * @param request
     * @return
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/profile")
    public R<Long> createUserProfile(
            @Valid @RequestBody UserProfileCreateDTO userProfileCreateDTO,
            HttpServletRequest request
    ) {
        return R.success(this.userProfileService.createUserProfile(userProfileCreateDTO, request));
    }
    /**
     * 删除用户配置文件类
     * @param userId
     * @param request
     * @return
     */
    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/profile/{userId}")
    public R<Void> deleteUserProfile(@PathVariable Long userId, HttpServletRequest request) {
        this.userProfileService.deleteUserProfile(userId, request);
        return R.success();
    }

    /**
     * 保存用户头像
     * @param multipartFile
     * @param request
     * @param userId
     * @return
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/saveUserAvatar")
    public R<String> saveUserAvatar(MultipartFile multipartFile, HttpServletRequest request, Long userId) {
        String avatarUrl = this.userProfileService.saveUserAvatar(multipartFile, userId, request);
        ThrowUtils.throwIf(avatarUrl == null || avatarUrl.isEmpty(), ErrorCode.UPLOAD_ERROR, "上传头像失败");
        return R.success(avatarUrl);
    }

    /**
     * 更新用户配置文件
     * @param userProfileUpdateDTO
     * @return
     */
    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/updateUserProfile")
    public R<Boolean> updateUserProfile(UserProfileUpdateDTO userProfileUpdateDTO,HttpServletRequest request) {
        boolean result = this.userProfileService.updateUserProfile(userProfileUpdateDTO,request);
        return R.success(result);
    }

    /**
     * 获取自身的用户配置文件
     * @param request
     * @return
     */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/profile/myself")
    public R<UserProfileVO> getUserProfileBySelf(HttpServletRequest request) {
        return R.success(this.userProfileService.getUserProfileBySelf(request));
    }

    /**
     * 根据用户id查询配置文件
     * @param userId
     * @return
     */
    @
            PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/profile/{userId}")
    public R<UserProfileVO> getUserProfileByUserId(@PathVariable  Long userId) {
        return R.success(this.userProfileService.getUserProfileByUserId(userId));
    }

    // ==================== 账户封禁部分 ====================

    /**
     * 封禁帐户
     * @param blockedUserDTO
     * @return
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/blockedUser")
    public R<Void> blockedUser(@RequestBody BlockedUserDTO  blockedUserDTO) {
        this.userService.blockedUser(blockedUserDTO);
        return R.success();
    }

    /**
     * 删除redis中的权限数据
     * @param userId
     * @return
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/deleteUserSecurity/{userId}")
    public R<Boolean> deleteUserSecurity(@PathVariable  Long userId) {
        this.userService.deleteUserSecurity(userId);
        return R.success(true);
    }

}
