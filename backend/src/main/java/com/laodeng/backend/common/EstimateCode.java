package com.laodeng.backend.common;

import lombok.Getter;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/8/2 13:01
 * @description
 */
@Getter
public enum EstimateCode {

    SUCCEED(1, "成功"),
    FAIL(0, "失败");

    private final int estimateCode;

    private final String message;

    EstimateCode(int estimateCode, String message) {
        this.estimateCode = estimateCode;
        this.message = message;
    }
}
