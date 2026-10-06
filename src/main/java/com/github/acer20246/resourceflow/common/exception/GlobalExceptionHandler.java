package com.github.acer20246.resourceflow.common.exception;

import com.github.acer20246.resourceflow.common.result.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler{
    /**
     * 处理业务异常
     * @param e
     * @return
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e){
        return Result.error(e.getMessage(), HttpStatus.BAD_REQUEST.value());
    }

    /**
     * 参数校验异常处理
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> HandleMethodArgumentNotValidException(MethodArgumentNotValidException e){
        String msg = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error->error.getDefaultMessage())
                .orElse("参数校验失败");
        return Result.error(msg, HttpStatus.BAD_REQUEST.value());
    }

    /**
     * 请求json格式异常
     * @param e
     * @return
     */

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> HandleHttpMessageNotReadableException(HttpMessageNotReadableException e){
        return Result.error("请求参数异常", HttpStatus.BAD_REQUEST.value());
    }
}
