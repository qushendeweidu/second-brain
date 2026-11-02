package com.laodeng.backend.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.laodeng.backend.common.ErrorCode;
import com.laodeng.backend.config.properties.TokenProperties;
import com.laodeng.backend.controller.github.GitHubBindingController;
import com.laodeng.backend.domain.dto.UserSocialInsertDTO;
import com.laodeng.backend.domain.po.User;
import com.laodeng.backend.domain.po.UserSocial;
import com.laodeng.backend.exception.BusinessException;
import com.laodeng.backend.exception.ThrowUtils;
import com.laodeng.backend.handler.GitHubLoginHandle;
import com.laodeng.backend.mapper.UserMapper;
import com.laodeng.backend.mapper.UserSocialMapper;
import com.laodeng.backend.service.UserService;
import com.laodeng.backend.service.UserSocialService;
import com.laodeng.backend.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.net.http.HttpRequest;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/10/22 20:15
 * @description 用户身份绑定业务层实现类
 */

@Log4j2
@Service
@RequiredArgsConstructor
public class UserSocialServiceImpl extends ServiceImpl<UserSocialMapper, UserSocial> implements UserSocialService {
    private final JwtUtils jwtUtils;
    private final TokenProperties tokenProperties;
    private final UserSocialMapper userSocialMapper;
    private final GitHubLoginHandle gitHubLoginHandle;
    private final String provide = "github";


    public record GithubCallbackReq(String code, String state) {}

    /**
     * 利用请求中携带的请求头中解析的token直接解析获得用户id之后完成绑定
     */
    public void bindWithGitHub(GithubCallbackReq githubCallbackReq){
        // 从请求中携带的token中解析出用户id
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long)authentication.getPrincipal();
        ThrowUtils.throwIf(ObjectUtil.isEmpty(userId),ErrorCode.NOT_LOGIN_ERROR);
        GitHubLoginHandle.GithubUser githubUser = this.gitHubLoginHandle.githubCallback(userId, githubCallbackReq.code(), githubCallbackReq.state());
        UserSocial existing = this.userSocialMapper.selectByProviderUid(this.provide,githubUser.id().toString());
        if (ObjectUtil.isNotEmpty(existing)){
            if (ObjectUtil.equal(existing.getUserId(),userId)){
                log.info("本人已经登录过了，发生了重复登录");
                return;
            }
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        //接下来判断当前用户是不是登录着其他的github账户
        ThrowUtils.throwIf(ObjectUtil.isNotEmpty(this.userSocialMapper.selectByUserAndProvider(userId,this.provide)),ErrorCode.OPERATION_ERROR);
        try {
            this.userSocialMapper.insert(
                    UserSocialInsertDTO.builder()
                            .userId(userId)
                            .provider(this.provide)
                            .providerUid(githubUser.id().toString())
                            .providerLogin(githubUser.login())
                            .build()
            );
        } catch (DuplicateKeyException e) {
            // 并发下两个请求同时通过上面的检查，唯一索引拦下第二个
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
    }

    /**
     * 解除github对于该用户的绑定
     */
    public void unbind() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long)authentication.getPrincipal();
        ThrowUtils.throwIf(ObjectUtil.isEmpty(userId),ErrorCode.NOT_LOGIN_ERROR);
        this.userSocialMapper.deleteByUserAndProvider(userId, this.provide);
    }

}
