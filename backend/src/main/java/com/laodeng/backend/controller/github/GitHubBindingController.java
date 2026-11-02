package com.laodeng.backend.controller.github;

import cn.hutool.core.util.ObjectUtil;
import com.laodeng.backend.common.ErrorCode;
import com.laodeng.backend.config.properties.TokenProperties;
import com.laodeng.backend.exception.BusinessException;
import com.laodeng.backend.handler.GitHubLoginHandle;
import com.laodeng.backend.service.UserSocialService;
import com.laodeng.backend.service.impl.UserSocialServiceImpl;
import com.laodeng.backend.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/10/22 20:13
 * @description GitHub绑定身份的接口类
 */

@RestController
@RequestMapping("/auth/github")
@RequiredArgsConstructor
public class GitHubBindingController {
    private final GitHubLoginHandle gitHubLoginHandle;
    private final UserSocialService userSocialService;
    private final JwtUtils jwtUtils;
    private final TokenProperties tokenProperties;

    /**
     * 创建请求GitHub的URL由前端发起所需要的请求地址由该接口提供
     * @return
     */
    @GetMapping("/authorize")
    public String authorize() {
        return this.gitHubLoginHandle.githubLogin();   // 外层按你项目的统一响应格式包装
    }

    /**
     * 回调函数接口用于使GitHub返回的前端回调路径，之后由监听该路径的前端向后端发起请求请求该接口
     * @param githubCallbackReq
     */
    @PostMapping("/callback")
    public void callback(@RequestBody UserSocialServiceImpl.GithubCallbackReq githubCallbackReq) {
        if (ObjectUtil.isEmpty(githubCallbackReq.code()) || ObjectUtil.isEmpty(githubCallbackReq.state())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        this.userSocialService.bindWithGitHub(githubCallbackReq);   // 三道关卡全过，才落库
    }

    /**
     * 解除当前用户的所有绑定的身份
     */
    @DeleteMapping("/unbind")
    public void unbind() {
        this.userSocialService.unbind();
    }

}
