package com.laodeng.backend.exception;

import com.laodeng.backend.common.ErrorCode;
import lombok.Getter;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/7/31 11:49
 * @description 业务异常类
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final ErrorCode errorCode;

    /**
     * 只有报错码的业务报错
     * @param errorCode 错误码
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 包含报错码和报错信息的业务报错
     * @param errorCode 错误码
     * @param message 报错信息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

}
