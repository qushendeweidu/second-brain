package com.laodeng.backend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.laodeng.backend.domain.dto.UserProfileCreateDTO;
import com.laodeng.backend.domain.dto.UserProfileUpdateDTO;
import com.laodeng.backend.domain.po.UserProfile;
import com.laodeng.backend.domain.vo.UserProfileVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/8/6 12:36
 * @description 用户配置文件业务层接口
 */

public interface UserProfileService extends IService<UserProfile> {

    String saveUserAvatar(MultipartFile multipartFile, Long userId, HttpServletRequest request);

    boolean updateUserProfile(UserProfileUpdateDTO userProfileUpdateDTO, HttpServletRequest request);

    UserProfileVO getUserProfileByUserId(Long userId);

    UserProfileVO getUserProfileBySelf(HttpServletRequest request);

    List<UserProfileVO> listUserProfiles(HttpServletRequest request);

    Long createUserProfile(UserProfileCreateDTO userProfileCreateDTO, HttpServletRequest request);

    void deleteUserProfile(Long userId, HttpServletRequest request);
}
