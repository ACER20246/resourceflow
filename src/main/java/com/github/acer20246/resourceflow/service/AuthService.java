package com.github.acer20246.resourceflow.service;

import com.github.acer20246.resourceflow.dto.AuthLoginDTO;
import com.github.acer20246.resourceflow.dto.AuthRegisterDTO;
import com.github.acer20246.resourceflow.vo.AuthLoginVO;

public interface AuthService {
    /**
     * 登录
     *
     * @param loginDTO
     * @return
     */
    AuthLoginVO login(AuthLoginDTO loginDTO);

    /**
     * 注册功能
     * @param registerDTO
     */
    void register(AuthRegisterDTO registerDTO);
}
