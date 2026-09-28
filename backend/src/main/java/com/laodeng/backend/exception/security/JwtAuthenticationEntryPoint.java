package com.laodeng.backend.exception.security;

import cn.hutool.core.util.ObjectUtil;
import com.laodeng.backend.common.ErrorCode;
import com.laodeng.backend.common.R;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/9/17 00:06
 * @description JWT
 */

@Log4j2
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper; // 序列化工具会将java类序列化
    /**
     * 认证失败统一入口（Filter 层异常的统一出口）
     * 所有 AuthenticationException 都会走到这里
     * @param request that resulted in an <code>AuthenticationException</code>
     * @param response so that the user agent can begin authentication
     * @param authException that caused the invocation
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {

        // 1. 优先取 Filter 里通过 request.setAttribute 存的错误码
        ErrorCode errorCode = (ErrorCode) request.getAttribute("ERROR_CODE");
        String message = (String) request.getAttribute("ERROR_MESSAGE");

        // 2. 兜底：如果 Filter 没设置，用默认 401
        if (ObjectUtil.isEmpty(errorCode)) {
            errorCode = ErrorCode.TOKEN_ERROR;
        }
        if (message == null || message.isBlank()) {
            message = authException.getMessage();
        }

        log.error("认证失败: {}", message);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); //设置相应的状态
        response.setContentType("application/json"); // 设置上下文类型
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());  // 获取编码类型
        R<?> body = R.error(errorCode,message); // 发送包装好的异常相应
        objectMapper.writeValue(response.getOutputStream(), body);

    }
}
