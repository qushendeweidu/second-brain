package com.laodeng.backend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.laodeng.backend.controller.github.GitHubBindingController;
import com.laodeng.backend.domain.po.UserRestrict;
import com.laodeng.backend.domain.po.UserSocial;
import com.laodeng.backend.handler.GitHubLoginHandle;
import com.laodeng.backend.service.impl.UserSocialServiceImpl;
import jakarta.servlet.http.HttpServletRequest;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/10/22 20:14
 * @description 用户身份绑定业务层
 */


public interface UserSocialService extends IService<UserSocial> {

    void bindWithGitHub(UserSocialServiceImpl.GithubCallbackReq req);

    void unbind();

}
