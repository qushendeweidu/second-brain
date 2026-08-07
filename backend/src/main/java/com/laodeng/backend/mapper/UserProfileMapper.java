package com.laodeng.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laodeng.backend.domain.dto.UserDTO;
import com.laodeng.backend.domain.po.UserProfile;
import com.laodeng.backend.domain.vo.UserVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/8/6 12:36
 * @description 用户配置文件mapper
 */

public interface UserProfileMapper extends BaseMapper<UserProfile> {
}
