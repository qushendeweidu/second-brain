package com.laodeng.backend.exception;

import com.laodeng.backend.common.ErrorCode;
import lombok.Getter;

/**
 * 自定义异常类
 *
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final int code;

    /**
     * 包含报错码和报错信息的业务报错
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 只有报错码的业务报错
     * @param errorCode 错误码
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 包含报错码和报错信息的业务报错
     * @param errorCode 错误码
     * @param message 报错信息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

}
