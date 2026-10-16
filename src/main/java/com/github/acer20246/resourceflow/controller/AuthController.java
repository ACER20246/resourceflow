package com.github.acer20246.resourceflow.controller;

import com.github.acer20246.resourceflow.common.result.Result;
import com.github.acer20246.resourceflow.dto.AuthLoginDTO;
import com.github.acer20246.resourceflow.dto.AuthRegisterDTO;
import com.github.acer20246.resourceflow.service.AuthService;
import com.github.acer20246.resourceflow.vo.AuthLoginVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Slf4j
@Tag(name = "用户相关接口", description = "用户相关接口")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Tag(name = "登录", description = "用户登录接口")
    public Result<AuthLoginVO> login(@RequestBody AuthLoginDTO loginDTO) {
        log.info("用户登录：username={}", loginDTO.getUsername());
        AuthLoginVO authLoginVO = authService.login(loginDTO);
        return Result.success(authLoginVO);
    }

    @PostMapping("/register")
    @Tag(name = "注册", description = "用户注册接口")
    public Result<AuthLoginVO> register(@RequestBody AuthRegisterDTO registerDTO) {
        log.info("用户注册：username={}", registerDTO.getUsername());
        authService.register(registerDTO);
        return Result.success();
    }

}
