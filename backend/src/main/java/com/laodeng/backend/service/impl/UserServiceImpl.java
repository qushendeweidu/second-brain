package com.laodeng.backend.service.impl;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.laodeng.backend.common.ErrorCode;
import com.laodeng.backend.domain.dto.LoginDTO;
import com.laodeng.backend.domain.dto.UserDTO;
import com.laodeng.backend.domain.po.User;
import com.laodeng.backend.domain.vo.UserVO;
import com.laodeng.backend.exception.ThrowUtils;
import com.laodeng.backend.handler.RedisSecurityHandle;
import com.laodeng.backend.mapper.UserMapper;
import com.laodeng.backend.service.UserService;
import com.laodeng.backend.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/7/26 09:16
 * @description
 */

@Log4j2
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final RedisSecurityHandle redisSecurityHandle;

    /**
     * 登陆
     *
     * @param loginDTO 登陆信息
     * @return 登陆结果
     */
    @Override
    public String login(LoginDTO loginDTO) {
        log.info("用户:{} 尝试登陆", loginDTO.getUsername());
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, loginDTO.getUsername());
        User user = this.getOne(queryWrapper);
        log.info("用户:{} 登陆", user.getUsername());
        ThrowUtils.throwIf(ObjUtil.isEmpty( user), ErrorCode.NOT_FOUND_ERROR);
        String token = null;
        if (user.getPassword().equals(loginDTO.getPassword())){
            Long userId = user.getId();
            if (redisSecurityHandle.checkSecurityKey(userId.toString())){
                token = redisSecurityHandle.getSecurityKey(userId.toString());
            }else {
                token = jwtUtils.createToken(userId);
                redisSecurityHandle.createSecurityKey(userId.toString(), token);
            }
        }
        return token;
    }

    /**
     * 根据用户DTO获取用户VO
     *
     * @param userDTO 用户DTO
     * @return 用户VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<UserVO> getUserVOByUserDTO(UserDTO userDTO) {
        log.info("当前的请求体:{}", userDTO);
        return userMapper.getUserVOByUserDTO(userDTO);
    }


}
