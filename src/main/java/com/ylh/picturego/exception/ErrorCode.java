package com.ylh.picturego.exception;

import lombok.Getter;

/**
 * 错误码
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "ok"),
    PARAMS_ERROR(40000, "参数错误"),
    NOT_LOGIN_ERROR(40100, "未登录错误"),
    NO_AUTH_ERROR(40101, "无权限错误"),
    NOT_FOUND_ERROR(40400, "请求数据不存在"),
    FORBIDDEN_ERROR(40300, "禁止访问"),
    SYSTEM_ERROR(50000, "系统错误"),
    OPERATION_ERROR(50001, "操作失败");

    private final int code;  // 状态码
    private final String message;  // 提示信息

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
