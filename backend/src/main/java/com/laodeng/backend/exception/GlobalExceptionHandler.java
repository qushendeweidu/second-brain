package com.laodeng.backend.exception;



import com.laodeng.backend.common.ErrorCode;
import com.laodeng.backend.common.R;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Log4j2
public class GlobalExceptionHandler {

    /**
     * 请求体参数校验失败（@RequestBody + @Valid）
     * @param e 校验异常
     * @return 响应包装类
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<?> methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return R.error(ErrorCode.PARAMS_ERROR, message);
    }

    /**
     * 缺少必填请求参数（@RequestParam 未传）
     * @param e 缺参异常
     * @return 响应包装类
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<?> missingServletRequestParameterExceptionHandler(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getParameterName());
        return R.error(ErrorCode.PARAMS_ERROR, "缺少必填参数: " + e.getParameterName());
    }

    /**
     * 请求参数类型不匹配（如 /product/abc 期望 Long）
     * @param e 类型不匹配异常
     * @return 响应包装类
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<?> methodArgumentTypeMismatchExceptionHandler(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配: name={}, value={}", e.getName(), e.getValue());
        return R.error(ErrorCode.PARAMS_ERROR, "参数类型不正确: " + e.getName());
    }

    /**
     * 请求体解析失败（JSON 格式错误 / 请求体为空）
     * @param e 解析异常
     * @return 响应包装类
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<?> httpMessageNotReadableExceptionHandler(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return R.error(ErrorCode.PARAMS_ERROR, "请求体格式错误");
    }

    /**
     * 请求方法不支持（如对 GET 接口发 POST）
     * @param e 方法不支持异常
     * @return 响应包装类
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R<?> httpRequestMethodNotSupportedExceptionHandler(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMethod());
        return R.error(ErrorCode.PARAMS_ERROR, "请求方法不支持: " + e.getMethod());
    }

    /**
     * 上传文件超过大小限制
     * @param e 文件超限异常
     * @return 响应包装类
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public R<?> maxUploadSizeExceededExceptionHandler(MaxUploadSizeExceededException e) {
        log.warn("上传文件超过大小限制: {}", e.getMessage());
        return R.error(ErrorCode.UPLOAD_ERROR, "上传文件过大");
    }

    /**
     * 业务异常处理
     * @param e 业务异常
     * @return 响应包装类
     */
    @ExceptionHandler(BusinessException.class)
    public R<?> businessExceptionHandler(BusinessException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return new R<>(e.getCode(), e.getMessage());
    }

    /**
     * 运行时异常
     * @param e 运行时异常
     * @return 响应包装类
     */
    @ExceptionHandler(RuntimeException.class)
    public R<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return R.error(ErrorCode.SYSTEM_ERROR);
    }

    /**
     * 兜底异常（捕获所有未被上面处理的异常，含受检异常）
     * @param e 异常
     * @return 响应包装类
     */
    @ExceptionHandler(Exception.class)
    public R<?> exceptionHandler(Exception e) {
        log.error("Exception", e);
        return R.error(ErrorCode.SYSTEM_ERROR);
    }

}
