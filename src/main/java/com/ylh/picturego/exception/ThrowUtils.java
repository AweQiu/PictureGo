package com.ylh.picturego.exception;

/**
 * 异常处理工具类
 */
public class ThrowUtils {

    /**
     * 如果条件为真，则抛出运行时异常
     *
     * @param condition 条件
     * @param runtimeException 运行时异常
     */
    public static void throwException(boolean condition, RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }

    /**
     * 如果条件为真，则抛出业务异常
     *
     * @param condition 条件
     * @param errorCode 错误码
     */
    public static void throwException(boolean condition, ErrorCode errorCode) {
        throwException(condition, new BusinessException(errorCode));
    }

    /**
     * 如果条件为真，则抛出业务异常
     *
     * @param condition 条件
     * @param errorCode 错误码
     * @param message 错误信息
     */
    public static void throwException(boolean condition, ErrorCode errorCode, String message) {
        throwException(condition, new BusinessException(errorCode, message));
    }
}
