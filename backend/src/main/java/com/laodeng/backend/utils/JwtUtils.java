package com.laodeng.backend.utils;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.laodeng.backend.common.ErrorCode;
import com.laodeng.backend.config.properties.JwtProperties;
import com.laodeng.backend.domain.po.UserRole;
import com.laodeng.backend.exception.ThrowUtils;
import com.laodeng.backend.service.UserRoleService;
import io.jsonwebtoken.Claims;
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
    private final SecretKey accessSecretKey;
    private final SecretKey refreshSecretKey;
    private final String refreshType;
    private final String accessType;

    /**
     * 初始化双 Token 各自的签名密钥（HS256 要求密钥至少 32 字节）
     *
     * @param userRoleService 用户角色服务，用于签发短时 Token 时查询角色权限
     * @param jwtProperties   JWT 配置，含两把独立密钥与两段有效期
     */
    @Autowired
    public JwtUtils(UserRoleService userRoleService, JwtProperties jwtProperties) {
        this.userRoleService = userRoleService;
        this.jwtProperties = jwtProperties;
        this.accessSecretKey = Keys.hmacShaKeyFor(jwtProperties.getAccessSecret().getBytes(StandardCharsets.UTF_8));
        this.refreshSecretKey = Keys.hmacShaKeyFor(jwtProperties.getRefreshSecret().getBytes(StandardCharsets.UTF_8));
        this.refreshType = "refresh";
        this.accessType = "access";

    }

    /**
     * 使用长时token交换获得短时token
     * @param refreshToken
     * @return
     */
    public String exchangeToken(String refreshToken){
        Claims claims = Jwts.parser()
                .verifyWith(this.refreshSecretKey)
                .build()
                .parseSignedClaims(refreshToken)
                .getPayload();
        String typ = String.valueOf(claims.get("typ"));
        ThrowUtils.throwIf(ObjectUtil.notEqual(typ,this.refreshType),ErrorCode.TOKEN_ERROR);
        Long userId = Long.parseLong(claims.getSubject());
        ThrowUtils.throwIf(ObjectUtil.isEmpty(userId),ErrorCode.NO_AUTH_ERROR);
        return createAccessToken(userId);
    }

    /**
     * 创建长时Token（Refresh Token）
     *
     * @param id 用户id
     * @return 只携带用户身份与刷新类型的 JWT 字符串
     */
    public String createRefreshToken(Long id) {
        return Jwts.builder()
                .header()
                .type("JWT")
                .and()
                .subject(String.valueOf(id)) // 将用户的id添加到令牌中
                .issuer(this.jwtProperties.getIssuer()) // 签发者
                .claim("typ", this.refreshType)
                .issuedAt(new Date()) // 签发时间
                .expiration(new Date(System.currentTimeMillis() + this.jwtProperties.getLongExpiration())) // 令牌过期时间
                .signWith(this.refreshSecretKey)
                .compact();
    }

    /**
     * 创建短时Token（Access Token），携带用户id、角色与权限
     *
     * @param id 用户id
     * @return 携带角色权限的访问型 JWT 字符串
     */
    public String createAccessToken(Long id) {
        LambdaQueryWrapper<UserRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserRole::getUserId, id);
        UserRole userRole = this.userRoleService.getOne(queryWrapper);
        ThrowUtils.throwIf(ObjectUtil.isEmpty(userRole), ErrorCode.NO_ROLE_ERROR);
        return Jwts.builder()
                .header()
                .type("JWT")
                .and()
                .subject(String.valueOf(id)) // 将用户的id添加到令牌中
                .issuer(this.jwtProperties.getIssuer()) // 签发者
                .claim("typ", this.accessType)
                .claim("roles", userRole.getRoles()) // 添加角色
                .claim("permissions", userRole.getPermissions()) // 添加权限
                .issuedAt(new Date()) // 签发时间
                .expiration(new Date(System.currentTimeMillis() + this.jwtProperties.getShortExpiration())) // 令牌过期时间
                .signWith(this.accessSecretKey)
                .compact();
    }

    /**
     * 从长时Token中提取用户id
     *
     * @param token 长时Token
     * @return 用户id；token 非法、过期或 subject 非数字时返回 null
     */
    public Long extractRefreshId(String token) {
        return parseSubject(token, true);
    }

    /**
     * 从短时Token中提取用户id
     *
     * @param token 短时Token
     * @return 用户id；token 非法、过期或 subject 非数字时返回 null
     */
    public Long extractAccessId(String token) {
        return parseSubject(token, false);
    }

    /**
     * 从短时Token中提取用户角色
     *
     * @param token 短时Token
     * @return 角色列表
     */
    public List<String> extractRoles(String token) {
        return extractAndReturnAccessClaims(token).get("roles", List.class);
    }

    /**
     * 从短时Token中提取用户权限
     *
     * @param token 短时Token
     * @return 权限列表
     */
    public List<String> extractPermissions(String token) {
        return extractAndReturnAccessClaims(token).get("permissions", List.class);
    }

    /**
     * 解析长时Token的全部声明，并校验 Token 类型
     *
     * @param token 长时Token
     * @return 声明集合
     * @throws JwtException 签名、有效期校验失败或 typ 不是 refresh 时抛出
     */
    public Claims extractAndReturnRefreshClaims(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(this.refreshSecretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (ObjectUtil.notEqual(this.refreshType, claims.get("typ", String.class))) {
            throw new JwtException("Token类型错误");
        }
        return claims;
    }

    /**
     * 解析短时Token的全部声明，并校验 Token 类型
     *
     * @param token 短时Token
     * @return 声明集合
     * @throws JwtException 签名、有效期校验失败或 typ 不是 access 时抛出
     */
    public Claims extractAndReturnAccessClaims(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(this.accessSecretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (ObjectUtil.notEqual(this.accessType, claims.get("typ", String.class))) {
            throw new JwtException("Token类型错误");
        }
        return claims;
    }

    /**
     * 校验长时Token是否有效（签名、有效期、typ 均通过解析校验）
     *
     * @param token 长时Token
     * @return true 表示基础 JWT 校验通过
     */
    public boolean isTokenValid(String token) {
        try {
            extractAndReturnRefreshClaims(token);
            return true;
        } catch (Exception e) {
            log.error("校验长时Token失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 校验短时Token是否有效（签名、有效期、typ 均通过解析校验）
     *
     * @param token 短时Token
     * @return true 表示基础 JWT 校验通过
     */
    public boolean isAccessTokenValid(String token) {
        try {
            extractAndReturnAccessClaims(token);
            return true;
        } catch (Exception e) {
            log.error("校验短时Token失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从长时Token中取出 Claims 后可自定义提取字段
     *
     * @param token          长时Token
     * @param claimsResolver 声明字段提取函数
     * @return 提取结果
     */
    public <T> T extractrefreshClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAndReturnRefreshClaims(token));
    }

    /**
     * 从短时Token中取出 Claims 后可自定义提取字段
     *
     * @param token          短时Token
     * @param claimsResolver 声明字段提取函数
     * @return 提取结果
     */
    public <T> T extractAccessClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAndReturnAccessClaims(token));
    }

    /**
     * 解析 Token 的 subject 并转换为用户id
     *
     * @param token       待解析 Token
     * @param refresh     true 按长时Token解析，false 按短时Token解析
     * @return 用户id；解析失败返回 null
     */
    private Long parseSubject(String token, boolean refresh) {
        try {
            Claims claims = refresh ? extractAndReturnRefreshClaims(token) : extractAndReturnAccessClaims(token);
            String subject = claims.getSubject();
            if (ObjectUtil.isEmpty(subject)) {
                return null;
            }
            Long id = Long.valueOf(subject);
            log.info("web_id:{}", id);
            return id;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("从令牌中提取用户ID失败: {}", e.getMessage());
            return null;
        }
    }
}
