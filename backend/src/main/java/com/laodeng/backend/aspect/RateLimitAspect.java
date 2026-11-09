package com.laodeng.backend.aspect;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.laodeng.backend.domain.po.UserRestrict;
import com.laodeng.backend.handler.RedisSecurityHandle;
import com.laodeng.backend.service.UserRestrictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/11/2 15:45
 * @description 请求限流切面类
 */

@Aspect
@Component
@Log4j2
@RequiredArgsConstructor
public class RateLimitAspect {

    private final UserRestrictService userRestrictService;
    private final RedisSecurityHandle redisSecurityHandle;

    @Pointcut("@annotation(com.laodeng.backend.annotation.RateLimit)")
    public void rateLimitCutPoint(){}

    @Around("rateLimitCutPoint()")
    public Object rateLimitHandle(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
    Long userId = currentUserId();
        if (ObjectUtil.isNotEmpty(userId)) {
        UserRestrict restrict = this.userRestrictService.getOne(
                new LambdaQueryWrapper<UserRestrict>().eq(UserRestrict::getUserId, userId));
        if (ObjectUtil.isNotEmpty(restrict)) {
            try {
                // 超限时内部抛 BusinessException(40208)，交给全局异常处理器返回标准 R
                this.redisSecurityHandle.restrictUserToken(
                        String.valueOf(userId),
                        restrict.getRestrictRequest(),
                        restrict.getWindowSecond(),
                        restrict.getTimeUnit()!=null?restrict.getTimeUnit():TimeUnit.SECONDS);
            } catch (RedisConnectionFailureException | RedisSystemException e) {
                // Redis 不可用时降级放行，避免限流故障拖垮正常请求
                log.warn("限流检查失败，降级放行: userId={}, err={}", userId, e.getMessage());
            }
        }
    }
        return proceedingJoinPoint.proceed();
}



    /** 从 SecurityContext 取当前登录用户 userId） */
    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        return (Long) principal;
    }

}
