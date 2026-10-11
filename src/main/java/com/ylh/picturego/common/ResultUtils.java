package com.ylh.picturego.common;

import com.ylh.picturego.exception.ErrorCode;

/**
 * 响应结果工具类
 */
public class ResultUtils {

    /**
     * 成功响应封装
     * @param data
     * @param <T>
     * @return
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, data, "ok");
    }

    /**
     * 失败响应封装
     * @param errorCode
     * @return
     */
    public static Result<?> error(ErrorCode errorCode) {
        return new Result<>(errorCode);
    }

    /**
     * 失败响应封装
     * @param code
     * @param message
     * @return
     */
    public static Result<?> error(int code, String message) {
        return new Result<>(code, null, message);
    }

    /**
     * 失败响应封装
     * @param errorCode
     * @param message
     * @return
     */
    public static Result<?> error(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), null, message);
    }
}
