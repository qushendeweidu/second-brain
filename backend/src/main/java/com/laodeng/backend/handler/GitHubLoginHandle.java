package com.laodeng.backend.handler;

import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.laodeng.backend.common.ErrorCode;
import com.laodeng.backend.config.properties.GitHubOAuthAppProperties;
import com.laodeng.backend.config.properties.TokenProperties;
import com.laodeng.backend.exception.BusinessException;
import com.laodeng.backend.exception.ThrowUtils;
import com.laodeng.backend.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/10/10 23:27
 * @description GitHub登录封装操作
 */

@Log4j2
@Component
public class GitHubLoginHandle {
    private final RestClient gitHubClient;
    private final GitHubOAuthAppProperties gitHubOAuthAppProperties;
    private final String AUTHOTIZEURL;
    private final String TOKEN_URL;
    private final String USER_URL;
    private final SecureRandom RANDOM;
    private final String PREFIX;
    private final String VERPREFIX;
    private final RedisSecurityHandle redisSecurityHandle;
    private final int generateStateLength;
    private final TokenProperties tokenProperties;
    private final JwtUtils jwtUtils;

    @Autowired
    public GitHubLoginHandle(GitHubOAuthAppProperties gitHubOAuthAppProperties,
                             @Qualifier("githubRestClient") RestClient restClient,
                             RedisSecurityHandle redisSecurityHandle,
                             TokenProperties tokenProperties,
                             JwtUtils jwtUtils
    ) {
        this.gitHubOAuthAppProperties = gitHubOAuthAppProperties;
        this.redisSecurityHandle = redisSecurityHandle;
        this.gitHubClient = restClient;
        this.RANDOM = new SecureRandom();
        this.PREFIX = "oauth:state:";
        this.VERPREFIX = "oauth:verifier:";
        this.AUTHOTIZEURL= "https://github.com/login/oauth/authorize";
        this.TOKEN_URL = "https://github.com/login/oauth/access_token";
        this.USER_URL = "https://api.github.com/user";
        this.generateStateLength = 32;
        this.tokenProperties = tokenProperties;
        this.jwtUtils = jwtUtils;
    }


    @JsonIgnoreProperties(ignoreUnknown = true)   // GitHub 多返回字段也不报错
    public record GithubTokenResponse(
            @JsonProperty("access_token") String accessToken,   // GitHub 用下划线，Java 用驼峰，靠注解映射
            @JsonProperty("token_type") String tokenType,
            String scope,
            String error,
            @JsonProperty("error_description") String errorDescription) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GithubUser(
            Long id,
            String login,
            String name,
            @JsonProperty("avatar_url") String avatarUrl) {}

    public String githubLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long)authentication.getPrincipal();
        ThrowUtils.throwIf(ObjectUtil.isEmpty(userId),ErrorCode.NOT_LOGIN_ERROR);
        // state密钥创建存入redis并返回
        String generateState = generateSecret(this.PREFIX+userId,userId);
        // verifier密钥创建并存入redis并返回
        String generateVerifier = generateSecret(this.VERPREFIX+userId,userId);
        return UriComponentsBuilder.fromUriString(this.AUTHOTIZEURL)
                .queryParam("client_id", this.gitHubOAuthAppProperties.getClientId())
                .queryParam("redirect_uri", this.gitHubOAuthAppProperties.getRedirectUri())
                .queryParam("state", generateState) // 一次性防伪密钥
                .queryParam("code_challenge",s256(generateVerifier))
                .queryParam("code_challenge_method", "S256")
                .build()
                .encode()      // 这一步做 URL 编码
                .toUriString();
    }

    public GithubUser githubCallback(Long userId, String code, String state) {
        String verifier = validate(userId, state);
        String accessToken = exchangeToken(code, verifier);
        return fetchUser(accessToken);
    }

    // 一次性消费用完即删校验
    public String validate(Long userId,String state) {
        if (ObjectUtil.isEmpty(userId)|| ObjectUtil.isEmpty(state)) {
            throw new BusinessException(ErrorCode.PARAMS_EMPTY_ERROR);
        }
        // 两个 key 都"读取并删除"
        String savedState = this.redisSecurityHandle.deleteAndGetSecurityKey(this.PREFIX + userId);
        String verifier = this.redisSecurityHandle.deleteAndGetSecurityKey(this.VERPREFIX + userId);
        ThrowUtils.throwIf(savedState == null || verifier == null ||
                        !MessageDigest.isEqual(state.getBytes(StandardCharsets.UTF_8),savedState.getBytes(StandardCharsets.UTF_8)),
                ErrorCode.VERIFIER_ERROR); // 过期、伪造、CSRF 都将异常在这里抛出
        return verifier;
    }

    public String exchangeToken(String code,String verifier){
        //创建一个Map之后给URL的参数放进post请求的请求body中
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", this.gitHubOAuthAppProperties.getClientId());
        form.add("client_secret", this.gitHubOAuthAppProperties.getClientSecret());
        form.add("code", code);
        form.add("redirect_uri", this.gitHubOAuthAppProperties.getRedirectUri());
        form.add("code_verifier", verifier);   // 钥匙在这里第一次出场

        GithubTokenResponse res;

        try {
            res = this.gitHubClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)   // 不加会返回表单字符串，解析失败
                    .body(form)
                    .retrieve()//从这里开始获得响应
                    .body(GithubTokenResponse.class);
        }catch (RestClientException e) {
            log.error("请求 GitHub 失败，检查网络和代理", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

        // code 错误或过期时 GitHub 仍返回 200，错误写在响应体里，不能只看状态码
        if (res == null || res.accessToken() == null) {
            //如果当前响应为空或者响应中的通关token为空
            log.warn("GitHub 换 token 失败: {}", res == null ? "无响应" : res.errorDescription());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return res.accessToken();

    }


    private GithubUser fetchUser(String accessToken) {
        GithubUser user;
        try {
            user = this.gitHubClient.get()
                    .uri(this.USER_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .retrieve()//从这里开始获得响应
                    .body(GithubUser.class);
        } catch (RestClientException e) {
            log.error("获取 GitHub 用户信息失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        if (user == null || user.id() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return user;
    }

    /** 生成并保存加密密钥，5 分钟过期 */
    public String generateSecret(String key , Long userId) {
        byte[] bytes = new byte[this.generateStateLength];
        this.RANDOM.nextBytes(bytes);
        //.withoutPadding()：去除填充字符（=）
        //Base64.getUrlEncoder()：URL 安全编码器
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        // 将当前用户的防伪字段存储进redis
        this.redisSecurityHandle.createOrUpdateSecurityKey(key, secret,5,TimeUnit.MINUTES);
        return secret;
    }

    private String s256(String verifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }



}
