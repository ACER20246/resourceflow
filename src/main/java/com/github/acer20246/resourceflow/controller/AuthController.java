package com.github.acer20246.resourceflow.controller;

import com.github.acer20246.resourceflow.common.result.Result;
import com.github.acer20246.resourceflow.dto.AuthLoginDTO;
import com.github.acer20246.resourceflow.dto.AuthRegisterDTO;
import com.github.acer20246.resourceflow.service.AuthService;
import com.github.acer20246.resourceflow.vo.AuthLoginVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Slf4j
@Tag(name = "用户相关接口", description = "用户相关接口")
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    @Tag(name = "登录", description = "用户登录接口")
    public Result login(@RequestBody AuthLoginDTO loginDTO) {
        log.info("用户登录：{}", loginDTO);
        AuthLoginVO authLoginVO = authService.login(loginDTO);
        return Result.success(authLoginVO);
    }

    @PostMapping("/register")
    @Tag(name = "注册", description = "用户注册接口")
    public Result register(@RequestBody AuthRegisterDTO registerDTO) {
        log.info("用户注册：{}", registerDTO);
        authService.register(registerDTO);
        return Result.success();
    }

}
