package com.github.acer20246.resourceflow.common.result;

import jakarta.annotation.Resource;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {
    @Override
    //Class<?> returnClass = returnType.getParameterType();获取返回数据类型。class
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        Class<?> returnClass = returnType.getParameterType();
        //已经是统一相应不包装
        if(Result.class.isAssignableFrom(returnClass)) {
            return false;
        }
        //ResponseEntity不包装,ResponseEntity 通常用于自己控制 HTTP 状态码、响应头、响应体
        if(ResponseEntity.class.isAssignableFrom(returnClass)) {
            return false;
        }
        //二进制资源不封装，Resource是spring的资源类，包含文件流、字节流等
        if(Resource.class.isAssignableFrom(returnClass)) {
            return false;
        }
        if (byte[].class.isAssignableFrom(returnClass)) {
            return false;
        }
        // String 使用 StringHttpMessageConverter,不能直接包装成 Result
        if (String.class.isAssignableFrom(returnClass)) {
            return false;
        }
        return true;
    }

    @Override
    public @Nullable Object beforeBodyWrite(@Nullable Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        return Result.success(body);
    }
}
