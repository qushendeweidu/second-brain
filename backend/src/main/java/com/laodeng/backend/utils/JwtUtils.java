package com.laodeng.backend.utils;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.laodeng.backend.common.ErrorCode;
import com.laodeng.backend.common.R;
import com.laodeng.backend.config.properties.JwtProperties;
import com.laodeng.backend.domain.po.UserRole;
import com.laodeng.backend.exception.ThrowUtils;
import com.laodeng.backend.service.UserRoleService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/7/28 15:06
 * @description JWT工具类，用于创建和解析JWT令牌
 */

@Log4j2
@Component
public class JwtUtils {
    private final UserRoleService userRoleService;
    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    @Autowired
    public JwtUtils(UserRoleService userRoleService, JwtProperties jwtProperties) {
        this.userRoleService = userRoleService;
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 创建Token
     */
    public String createToken(Long id) {
        LambdaQueryWrapper<UserRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserRole::getUserId, id);
        UserRole userRole = this.userRoleService.getOne(queryWrapper);
        ThrowUtils.throwIf(userRole == null || ObjUtil.isEmpty(userRole), ErrorCode.NO_ROLE_ERROR);
        return Jwts.builder()
                .header()
                .type("JWT")
                .and() // 创建JWT构建器
                .subject(String.valueOf(id)) // 将用户的id添加到令牌中
                .issuer(this.jwtProperties.getIssuer()) // 签发者
                .claim("roles", userRole.getRoles()) // 添加角色
                .claim("permissions", userRole.getPermissions()) // 添加权限
                .issuedAt(new Date()) // 签发时间
                .expiration(new Date(System.currentTimeMillis() + this.jwtProperties.getExpiration())) // 令牌过期时间
                .signWith(this.secretKey) // 向claim中添加密钥
                .compact();
    }


    //解析获得关键Id
    public Long extractId(String token) {
        try {
            String subject = extractClaim(token, Claims::getSubject);
            if (subject == null || subject.isEmpty()) {
                return null;
            }
            Long Id = Long.valueOf(subject);
            log.info("web_id:{}", Id);
            return Id;
        } catch (Exception e) {
            log.debug("从令牌中提取WebId时发生异常", e);
            return null;
        }
    }

    /**
     * 从token中提取用户角色
     * @param token
     * @return List<String>
     */
    public List<String> extractRoles(String token) {
        return extractAndReturnClaims(token).get("roles", List.class);
    }

    /**
     * 从token中提取用户权限
     * @param token
     * @return List<String>
     */
    public List<String> extractPermissions(String token) {
        return extractAndReturnClaims(token).get("permissions", List.class);
    }

    //提取所有的声明
    public Claims extractAndReturnClaims(String token) {
        // 尝试提取token中的所有声明（Claims）
        try {
            // 当前JWT的token
            log.info("Token:{}", token);
            // 使用JWT库解析token
            return Jwts.parser()
                    .verifyWith(this.secretKey) // 设置密钥
                    .build()
                    .parseSignedClaims(token) // 解析并验证token
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        } catch (JwtException e) {
            throw e;
        }
    }

    // 验证当前token是否过期
    public boolean isTokenValid(String token) {
        // 用于判断当前token是否过期
        try {
            return extractExpiration(token).after(new Date());
        } catch (Exception e) {
            log.error("从令牌中提取过期时间时发生异常:{}", e.getMessage());
            return false;
        }
    }

    // 获取令牌过期时间
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // 提取Claim中的数据
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        // 调用extractAllClaims方法，传入令牌token，提取所有的声明信息
        final Claims claims = extractAndReturnClaims(token);
        // 使用传入的claimsResolver函数，对提取的声明信息进行处理，并返回处理后的结果
        return claimsResolver.apply(claims);
    }
}
