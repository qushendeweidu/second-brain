package com.laodeng.backend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.laodeng.backend.common.CustomPage;
import com.laodeng.backend.common.ErrorCode;
import com.laodeng.backend.common.PageResult;
import com.laodeng.backend.config.properties.TokenProperties;
import com.laodeng.backend.domain.dto.*;
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
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

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
    private final TokenProperties tokenProperties;
    private final UserProxyRepository userProxyRepository;
    private final RedisSecurityHandle redisSecurityHandle;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    /**
     * 登陆
     *
     * @param loginDTO 登陆信息
     * @return 登陆结果
     */
    @Override
    public Map<String, String> login(LoginDTO loginDTO, HttpServletRequest request) {
        log.info("用户:{} 尝试登陆", loginDTO.getUsername());
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, loginDTO.getUsername());
        User user = this.getOne(queryWrapper);
        ThrowUtils.throwIf(ObjectUtil.isEmpty(user), ErrorCode.NOT_FOUND_ERROR); //当前账户不存在时拦截
        ThrowUtils.throwIf(ObjectUtil.equal(user.getStatus(),0),ErrorCode.USER_BLOCKED); //当前账户被锁定时拦截
        // 检测当前token的userId的redis是否被短时封禁
        ThrowUtils.throwIf(ObjectUtil.equal(redisSecurityHandle.getSecurityKey(user.getId().toString()),"0"),ErrorCode.USER_BLOCKED);
        log.info("用户:{} 登陆", user.getUsername());
        Map<String,String> userToken = new HashMap<>(); // 用于存储长短Token的Map
        // 判断当前用户的密码是否正确
        ThrowUtils.throwIf(!this.passwordEncoder.matches(loginDTO.getPassword(), user.getPassword()),ErrorCode.PASSWORD_ERROR);
        // 用户登录通过创建用户的id
        Long userId = user.getId();
        String freshToken = request.getHeader(this.tokenProperties.getRefresh());
        String shortToken = request.getHeader(this.tokenProperties.getGeneration());
        if (ObjectUtil.isEmpty(freshToken) ||
                !this.jwtUtils.isTokenValid(freshToken) ||
                !ObjectUtil.equal(userId,this.jwtUtils.extractId(freshToken))) { // 判断当前是否携带长Token或者是否有效
            //如果未存在Token则直接创建一个新的长时Token
            String newFreshToken = this.jwtUtils.createToken(userId);
            // 将长Token放到Map中
            userToken.put(this.tokenProperties.getRefresh(),newFreshToken);
            // 更新redis中的长时token
            redisSecurityHandle.createOrUpdateSecurityKey(userId.toString(),newFreshToken);
            // 更新长时Token
            freshToken = newFreshToken;
        }
        if (ObjectUtil.isEmpty(shortToken) || !this.jwtUtils.isTokenValid(shortToken,freshToken)){ // 判断当前是否携带短Token或者是否有效
            //如果未存在Token则直接创建一个新的短时Token
            String newShortToken = this.jwtUtils.createToken(userId,freshToken);
            // 将短Token放到Map中
            userToken.put(this.tokenProperties.getGeneration(),newShortToken);
        }
        return userToken;
    }

    @Override
    @Transactional(rollbackFor = DuplicateKeyException.class)
    public Long createUser(UserCreateDTO userCreateDTO) {
        return this.userProxyRepository.createUser(userCreateDTO);
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
    public boolean updateUser(UserUpdateDTO userUpdateDTO) {
        User user = this.getById(userUpdateDTO.getId());
        ThrowUtils.throwIf(user == null, ErrorCode.USER_NOT_FOUND_ERROR);

        if (CharSequenceUtil.isNotBlank(userUpdateDTO.getUsername()) && !ObjectUtil.equal(user.getUsername(), userUpdateDTO.getUsername())) {
            try {
                user.setUsername(userUpdateDTO.getUsername());
            } catch (DuplicateKeyException e) {
                log.error("当前用户名重复触发唯一约束导致异常抛出给spring");
                throw new RuntimeException(e);
            }
        }
        if (CharSequenceUtil.isNotBlank(userUpdateDTO.getPassword())) {
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
     * @param registerDTO 登陆DTO层
     */
    @Override
    public void register(RegisterDTO registerDTO) {
        this.userProxyRepository.createUser(UserCreateDTO.builder()
                .username(registerDTO.getUsername())
                .password(registerDTO.getPassword())
                .build());
    }

    /**
     * 封禁账户
     * @param blockedUserDTO
     */
    @Override
    public void blockedUser(BlockedUserDTO blockedUserDTO) {
        ThrowUtils.throwIf(blockedUserDTO.getUserId() == null || ObjectUtil.isEmpty(blockedUserDTO.getUserId()), ErrorCode.PARAMS_EMPTY_ERROR);
        User user = this.getById(blockedUserDTO.getUserId());
        ThrowUtils.throwIf(ObjectUtil.isEmpty(user), ErrorCode.USER_NOT_FOUND_ERROR);
        if (ObjectUtil.isNotEmpty(blockedUserDTO.getBlockedTime())) {
            this.redisSecurityHandle.createOrUpdateSecurityKey(
                    blockedUserDTO.getUserId().toString(),
                    "0",
                    blockedUserDTO.getBlockedTime(),
                    blockedUserDTO.getTimeUnit()
            );
        }
        if (ObjectUtil.isNotEmpty(blockedUserDTO.getBlocked()) && Boolean.TRUE.equals(blockedUserDTO.getBlocked())) {
            if (ObjectUtil.equal(user.getStatus(), 0)) {
                log.info("当前账户已经被封禁");
            }
            LambdaUpdateWrapper<User> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
            lambdaUpdateWrapper.eq(User::getId, blockedUserDTO.getUserId())
                    .set(User::getStatus, 0);
            if (this.update(lambdaUpdateWrapper)){
                log.info("用户账户状态更新成功正在删除redis残余key");
                this.redisSecurityHandle.deleteSecurityKey(user.getId().toString());
            }
        }
    }

    /**
     * 根据用户DTO获取用户VO
     * @param userDTO 用户DTO
     * @return 用户VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PageResult<UserVO> getUserVOByUserDTO(UserDTO userDTO) {
        log.info("当前的请求体:{}", userDTO);
        Page<UserVO> page = new CustomPage<>(
                ObjectUtil.isEmpty(userDTO.getPageDTO())  // 当前用户的pageDTO为空
                        || userDTO.getPageDTO().getPageNum() == null  // 当前页码为空
                        || userDTO.getPageDTO().getPageNum() < 1 // 当前页码小于1
                        || userDTO.getPageDTO().getPageNum() > 100 // 当前页码大于100
                        ? 1 : userDTO.getPageDTO().getPageNum(),
                ObjectUtil.isEmpty(userDTO.getPageDTO())  // 当前用户的pageDTO为空
                        || userDTO.getPageDTO().getPageSize() == null // 当前每页数据量为空
                        || userDTO.getPageDTO().getPageSize() < 1 //当前每页数据量小于1
                        || userDTO.getPageDTO().getPageSize() > 100 // 当前每页数据量大于100
                        ? 10 : userDTO.getPageDTO().getPageSize()
        );
        IPage<UserVO> iPage = this.userMapper.getUserVOByUserDTO(page, userDTO);
        return PageResult.of(iPage.getRecords(), iPage.getTotal(), iPage.getCurrent(), iPage.getSize());
    }

    /**
     * 删除用户的权限
     * @param userId
     */
    @Override
    public void deleteUserSecurity(Long userId) {
        log.info("正在删除redis的权限数据");
        this.redisSecurityHandle.deleteSecurityKey(userId.toString());
    }

    /**
     * 根据长时token获取短时token
     * @param request
     * @return
     */
    public String getShortToken(HttpServletRequest request){
        String refreshToken = request.getHeader(tokenProperties.getRefresh());
        User user = this.getById(this.jwtUtils.extractId(refreshToken));
        // 首先判断当前用户是否已经被封号或者用户不存在
        ThrowUtils.throwIf(ObjectUtil.isEmpty(user)|| ObjectUtil.equal(user.getStatus(),0),ErrorCode.TOKEN_ERROR );
        // 若用户已经被暂时封禁则抛异常
        ThrowUtils.throwIf(ObjectUtil.equal(redisSecurityHandle.getSecurityKey(user.getId().toString()),"0"),ErrorCode.USER_BLOCKED);
        // 创建用户id的Long对象
        Long userId = user.getId();
        //判断当前的刷新token是否有效而且不为空
        ThrowUtils.throwIf(ObjectUtil.isEmpty(refreshToken) ||!this.jwtUtils.isTokenValid(refreshToken),ErrorCode.TOKEN_ERROR);
        // 当前长时token是有效的
        String shortToken = this.jwtUtils.createToken(userId,refreshToken);
        ThrowUtils.throwIf(ObjectUtil.isEmpty(shortToken),ErrorCode.TOKEN_CREATE_ERROR);
        return shortToken;

    }

}
