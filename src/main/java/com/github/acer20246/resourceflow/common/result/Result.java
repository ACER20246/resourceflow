package com.github.acer20246.resourceflow.common.result;


import com.github.acer20246.resourceflow.common.filter.TraceIdFilter;

/**
 * 后端统一返回结果
 * @param <T>
 */
public record Result<T>(
        Integer code,
        String message,
        T data,
        String traceId

){

    public static <T> Result<T> success() {
        return new Result<>(200, "success", null, TraceIdFilter.getTraceId());
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data, TraceIdFilter.getTraceId());
    }

    public static <T> Result<T> error(String msg,Integer code) {
        return new Result<>(code, msg, null, TraceIdFilter.getTraceId());
    }
}

