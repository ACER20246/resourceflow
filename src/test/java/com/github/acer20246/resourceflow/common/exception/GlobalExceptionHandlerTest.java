package com.github.acer20246.resourceflow.common.exception;

import com.github.acer20246.resourceflow.common.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link GlobalExceptionHandler} 单元测试：验证各类异常到统一响应结构的映射。
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /**
     * 仅用于构造 MethodParameter 的占位方法。
     */
    @SuppressWarnings("unused")
    private void placeholder() {
    }

    private MethodParameter buildMethodParameter() {
        Method method;
        try {
            method = GlobalExceptionHandlerTest.class.getDeclaredMethod("placeholder");
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
        return new MethodParameter(method, -1);
    }

    @Test
    @DisplayName("业务异常映射为 code 400 并携带原始消息")
    void handleBusinessException() {
        // act
        Result<Void> result = handler.handleBusinessException(new BusinessException("预约时间段已被占用"));

        // assert
        assertThat(result.code()).isEqualTo(400);
        assertThat(result.message()).isEqualTo("预约时间段已被占用");
        assertThat(result.data()).isNull();
    }

    @Test
    @DisplayName("参数校验异常取第一条字段错误消息")
    void handleMethodArgumentNotValidException() {
        // arrange
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "username", "用户名不能为空"));

        // act
        Result<Void> result = handler.HandleMethodArgumentNotValidException(
                new MethodArgumentNotValidException(buildMethodParameter(), bindingResult));

        // assert
        assertThat(result.code()).isEqualTo(400);
        assertThat(result.message()).isEqualTo("用户名不能为空");
        assertThat(result.data()).isNull();
    }

    @Test
    @DisplayName("参数校验异常无字段错误时返回“参数校验失败”")
    void handleMethodArgumentNotValidExceptionWithoutFieldErrors() {
        // arrange
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "target");

        // act
        Result<Void> result = handler.HandleMethodArgumentNotValidException(
                new MethodArgumentNotValidException(buildMethodParameter(), bindingResult));

        // assert
        assertThat(result.code()).isEqualTo(400);
        assertThat(result.message()).isEqualTo("参数校验失败");
    }

    @Test
    @DisplayName("请求体不可读异常映射为“请求参数异常”")
    void handleHttpMessageNotReadableException() {
        // act
        Result<Void> result = handler.HandleHttpMessageNotReadableException(
                new HttpMessageNotReadableException("bad json", new MockHttpInputMessage(new byte[0])));

        // assert
        assertThat(result.code()).isEqualTo(400);
        assertThat(result.message()).isEqualTo("请求参数异常");
        assertThat(result.data()).isNull();
    }

    @Test
    @DisplayName("未知异常映射为 code 500“服务器内部错误”")
    void handleUnexpectedException() {
        // act
        Result<Void> result = handler.handleUnexpectedException(new RuntimeException("boom"));

        // assert
        assertThat(result.code()).isEqualTo(500);
        assertThat(result.message()).isEqualTo("服务器内部错误");
        assertThat(result.data()).isNull();
    }
}