package com.laodeng.backend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.laodeng.backend.common.PageResult;
import com.laodeng.backend.domain.dto.*;
import com.laodeng.backend.domain.po.User;
import com.laodeng.backend.domain.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/7/26 09:16
 * @description 用户业务层接口
 */

public interface UserService extends IService<User> {

    PageResult<UserVO> getUserVOByUserDTO(UserDTO userDTO);

    UserVO getUserVOById(Long id);

    Long createUser(UserCreateDTO userCreateDTO);

    boolean updateUser(UserUpdateDTO userUpdateDTO);

    void deleteUser(Long id);

    Map<String, String> login(LoginDTO loginDTO, HttpServletRequest request);

    void register(RegisterDTO registerDTO);

    void blockedUser(BlockedUserDTO userId);

    void deleteUserSecurity(Long userId);

    String getShortToken(HttpServletRequest request);
}
