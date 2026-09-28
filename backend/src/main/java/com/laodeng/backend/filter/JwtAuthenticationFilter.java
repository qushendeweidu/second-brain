package com.laodeng.backend.filter;

import cn.hutool.core.util.ObjectUtil;
import com.laodeng.backend.common.ErrorCode;
import com.laodeng.backend.config.properties.TokenProperties;
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
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
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
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtils;
    private final UserService userService;
    private final RedisSecurityHandle redisSecurityHandle;
    private final List<String> authWhitelist;
    private final AntPathMatcher pathMatcher;
    private final TokenProperties tokenProperties;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, UserService userService, RedisSecurityHandle redisSecurityHandle, List<String> authWhitelist, TokenProperties tokenProperties) {
        this.jwtUtils = jwtUtils;
        this.userService = userService;
        this.redisSecurityHandle = redisSecurityHandle;
        this.authWhitelist = authWhitelist;
        this.tokenProperties = tokenProperties;
        this.pathMatcher = new AntPathMatcher();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        log.info("当前线程: {}", Thread.currentThread().getName());
        long startTime = System.currentTimeMillis();
        try {
            String shortToken = request.getHeader(tokenProperties.getGeneration()); // 从请求中获取短时token
            String refreshToken = request.getHeader(tokenProperties.getRefresh()); // 从请求中获取长Token
            //判断当前长短token是否为空一般情况下会首先去登录因为登录不会进入过滤器
            if (ObjectUtil.isEmpty(refreshToken) || ObjectUtil.isEmpty(shortToken)) {
                log.info("用户Token为空");
                throw new BusinessException(ErrorCode.TOKEN_ERROR, "用户Token为空");
            }
            // 校验当前传入的JWT token是否符合JWT标准
            ThrowUtils.throwIf(shortToken.split("\\.").length!=3 || refreshToken.split("\\.").length!=3,new BusinessException(ErrorCode.TOKEN_ERROR));
            //校验长时token是否有效
            ThrowUtils.throwIf(!this.jwtUtils.isTokenValid(refreshToken),ErrorCode.TOKEN_ERROR);
            // 短时token如果无效直接抛异常
            // （长时token作为短时token的密钥）
            ThrowUtils.throwIf(!this.jwtUtils.isTokenValid(shortToken,refreshToken),ErrorCode.TOKEN_ERROR);
            //短时Token校验无误之后
            Long userId = this.jwtUtils.extractId(refreshToken); // 从token中提取用户id，这里即使token过期也可以正常获取userId
            ThrowUtils.throwIf(ObjectUtil.isEmpty(userId), new BusinessException(ErrorCode.TOKEN_ERROR, "用户Token无效"));//如果用户ID为空那么抛出异常
            // 首先检测当前token的userId的redis是否被短时封禁
            ThrowUtils.throwIf(ObjectUtil.equal(redisSecurityHandle.getSecurityKey(userId.toString()),"0"),ErrorCode.USER_BLOCKED);

            // Security封装好的对象列表用于存储用户的权限和角色
            List<GrantedAuthority> authorities = new ArrayList<>();
            // 从token中获取用户角色
            this.jwtUtils.extractRoles(refreshToken).forEach(
                    role -> authorities.add(new SimpleGrantedAuthority("ROLE_"+role))
            );
            // 从token中获取用户权限
            this.jwtUtils.extractPermissions(refreshToken).forEach(
                    permission -> authorities.add(new SimpleGrantedAuthority(permission))
            );
            // 创建Authentication对象保存用户权限和用户Id
            Authentication authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            /*
            将Authentication对象设置到SecurityContextHolder中（主要目的是为了@PreAuthorize去识别用户的权限和后续可以直接使用下方的这个方法调用到存储的数据）
              Authentication auth = SecurityContextHolder.getContext().getAuthentication();
              Object principal = auth.getPrincipal(); // 就是 userId
             */
            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setAttribute("userId", userId); //将当前的用户id存入到请求中
            filterChain.doFilter(request, response);
        }catch (AuthenticationCredentialsNotFoundException e) {
            // 标准认证异常 → 交给 Spring Security，会路由到 EntryPoint
            throw e;
        } catch (BusinessException e) {
            // 非认证类业务异常，也转成认证异常（或按需处理）
            log.error("业务异常: {}", e.getMessage());
            setErrorAttributes(request, e.getErrorCode(), e.getMessage());
            throw new AuthenticationCredentialsNotFoundException(e.getMessage(), e);
        } finally {
            log.info("请求执行完毕请求耗时: {} ms", System.currentTimeMillis() - startTime);
            log.info("当前线程: {} 请求完毕清除线程数据", Thread.currentThread().getName());

        }
    }

    /** 把错误码存入 request，供 EntryPoint 读取 */
    private void setErrorAttributes(HttpServletRequest request, ErrorCode errorCode, String message) {
        request.setAttribute("ERROR_CODE", errorCode); // 将异常枚举类放到request中方便JwtAuthenticationEntryPoint中的读取并序列化
        request.setAttribute("ERROR_MESSAGE", message); // 将异常信息放到request中方便JwtAuthenticationEntryPoint中的读取并序列化
    }

    /**
     * 当前方法作用在于检查当前提供的UserId的对应的用户是否存在账户是否正常
     * @param userId 用户唯一ID
     * @return 如果正常则返回true 如果失败则直接抛出异常
     */
    private Boolean checkUserActive(Long userId){
        User user = this.userService.getById(userId);
        ThrowUtils.throwIf(ObjectUtil.isEmpty(user),ErrorCode.USER_NOT_FOUND_ERROR);
        ThrowUtils.throwIf(ObjectUtil.equal(user.getStatus(),0),ErrorCode.USER_BLOCKED);
        return true;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 获取进入拦截器的请求的请求路径
        String path = request.getServletPath();
        // anyMatcher表示的是否有一个符合也就这里意思就是当前请求路径是否符合白名单中的任意一个路径
        return this.authWhitelist.stream().anyMatch(p -> pathMatcher.match(p, path));
    }
}
