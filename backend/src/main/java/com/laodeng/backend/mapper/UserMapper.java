package com.laodeng.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laodeng.backend.domain.dto.UserDTO;
import com.laodeng.backend.domain.po.User;
import com.laodeng.backend.domain.vo.UserVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/7/26 09:42
 * @description 用户Mapper
 */

public interface UserMapper extends BaseMapper<User> {
    List<UserVO> getUserVOByUserDTO(@Param("userDTO") UserDTO userDTO);
}
