package com.laodeng.backend.filter;

import cn.hutool.core.util.ObjUtil;
import com.laodeng.backend.common.ErrorCode;
import com.laodeng.backend.domain.po.User;
import com.laodeng.backend.exception.BusinessException;
import com.laodeng.backend.exception.ThrowUtils;
import com.laodeng.backend.handler.RedisSecurityHandle;
import com.laodeng.backend.service.UserService;
import com.laodeng.backend.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/7/28 13:00
 * @description Jwt结合security的拦截器
 */

@Log4j2
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtils;
    private final UserService userService;
    private final RedisSecurityHandle redisSecurityHandle;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("当前线程: {}", Thread.currentThread().getName());
        Long startTime = System.currentTimeMillis();
        try {
            String token = request.getHeader("Authorization"); // 从请求头中获取token
            ThrowUtils.throwIf(token.split("\\.").length>3,new BusinessException(ErrorCode.TOKEN_ERROR));
            if (token == null || token.isEmpty()) {
                log.info("用户Token无效");
                throw new BusinessException(ErrorCode.TOKEN_ERROR, "用户Token无效");
            }
            Long userId = this.jwtUtils.extractId(token); // 从token中提取用户id
            // 首先判断当前redis是否存在该用户的JWT如果不存在就抛出异常让前端去跳转登录页面
            ThrowUtils.throwIf(!this.redisSecurityHandle.checkSecurityKey(userId.toString()), new BusinessException(ErrorCode.TOKEN_ERROR, "用户Token不存在"));
            if (!this.jwtUtils.isTokenValid(token)) {
                log.info("用户Token过期");
                User user = this.userService.getById(userId);
                if (ObjUtil.isNull(user)) {
                    log.info("用户不存在");
                    throw new BusinessException(ErrorCode.USER_NOT_FOUND_ERROR, "用户不存在");
                } else if (ObjUtil.equals(user.getId(), userId)) {
                    log.info("用户确实存在开始创建新的jwt");
                    token = this.jwtUtils.createToken(userId);
                    this.redisSecurityHandle.createSecurityKey(userId.toString(), token);
                } else {
                    throw new BusinessException(ErrorCode.USER_NOT_FOUND_ERROR, "用户不存在");
                }
            }
            log.info("当前Token: {}", token);
            List<GrantedAuthority> authorities = new ArrayList<>();
            // 从token中获取用户角色
            this.jwtUtils.extractRoles(token).forEach(
                    role -> authorities.add(new SimpleGrantedAuthority("ROLE_"+role))
            );
            // 从token中获取用户权限
            this.jwtUtils.extractPermissions(token).forEach(
                    permission -> authorities.add(new SimpleGrantedAuthority(permission))
            );
            // 创建Authentication对象
            Authentication authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            // 将Authentication对象设置到SecurityContextHolder中
            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setAttribute("userId", userId); //将当前的用户id存入到请求中
            filterChain.doFilter(request, response);
        } finally {
            log.info("请求执行完毕请求耗时: {} ms", System.currentTimeMillis() - startTime);
            log.info("当前线程: {} 请求完毕清除线程数据", Thread.currentThread().getName());
        }
    }

}
