package com.laodeng.backend.common;

import lombok.Getter;

/**
 * 错误码规则：
 * 第1位 — 错误类别（4:客户端错误, 5:服务端错误）
 * 第2-3位 — 模块编号（00:通用, 01:认证, 02:用户, 03:文件）
 * 第4-5位 — 模块内序号
 */
@Getter
public enum ErrorCode {

    // ==================== 成功 ====================
    SUCCESS(200, "ok"),

    // ==================== 客户端错误 - 通用 (400xx) ====================
    PARAMS_ERROR(40001, "请求参数错误"),
    NOT_FOUND_ERROR(40002, "请求数据不存在"),
    FORBIDDEN_ERROR(40003, "禁止访问"),
    PARAMS_EMPTY_ERROR(40004, "请求参数为空"),

    // ==================== 客户端错误 - 认证模块 (401xx) ====================
    NOT_LOGIN_ERROR(40101, "未登录"),
    TOKEN_EXPIRED_ERROR(40102, "token已过期"),
    TOKEN_ERROR(401,"无效token"),
    NO_AUTH_ERROR(40103, "无权限"),
    NO_ROLE_ERROR(40104, "无角色权限"),

    // ==================== 客户端错误 - 用户模块 (402xx) ====================
    LOGIN_ERROR(40201, "登录失败"),
    REGISTER_ERROR(40202, "注册失败"),
    USER_NOT_FOUND_ERROR(40203, "用户不存在"),
    PASSWORD_ERROR(40204, "密码错误"),
    USER_UPDATE_ERROR(40204, "用户信息更新失败"),

    // ==================== 客户端错误 - 文件模块 (403xx) ====================
    UPLOAD_ERROR(40301, "文件上传失败"),
    DOWNLOAD_ERROR(40302, "文件下载失败"),
    FILE_NOT_FOUND_ERROR(40303, "文件不存在"),
    ACHIEVE_FILE_URL_ERROR(40304,"文件路径获取异常"),

    // ==================== 服务端错误 - 系统级 (500xx) ====================
    SYSTEM_ERROR(50001, "系统内部异常"),
    OPERATION_ERROR(50002, "操作失败"),
    SERIALIZE_ERROR(50003, "序列化失败"),
    DESERIALIZE_ERROR(50004, "反序列化失败"),
    // ==================== 服务端错误 - 系统级 (500xx) ====================
    BUSINESS_ERROR(50005, "业务错误");

    private final int code;

    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}