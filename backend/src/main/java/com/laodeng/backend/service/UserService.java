package com.laodeng.backend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.laodeng.backend.domain.dto.LoginDTO;
import com.laodeng.backend.domain.dto.UserCreateDTO;
import com.laodeng.backend.domain.dto.UserDTO;
import com.laodeng.backend.domain.dto.UserUpdateDTO;
import com.laodeng.backend.domain.po.User;
import com.laodeng.backend.domain.vo.UserVO;

import java.util.List;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/7/26 09:16
 * @description 用户业务层接口
 */

public interface UserService extends IService<User> {

    List<UserVO> getUserVOByUserDTO(UserDTO userDTO);

    UserVO getUserVOById(Long id);

    Long createUser(UserCreateDTO userCreateDTO);

    boolean updateUser(UserUpdateDTO userUpdateDTO);

    void deleteUser(Long id);

    String login(LoginDTO loginDTO);

    void register(LoginDTO loginDTO);
}
