package com.github.acer20246.resourceflow.service.Impl;

import com.github.acer20246.resourceflow.common.utils.JwtUtils;
import com.github.acer20246.resourceflow.dto.AuthLoginDTO;
import com.github.acer20246.resourceflow.dto.AuthRegisterDTO;
import com.github.acer20246.resourceflow.entity.User;
import com.github.acer20246.resourceflow.mapper.AuthMapper;
import com.github.acer20246.resourceflow.service.AuthService;
import com.github.acer20246.resourceflow.vo.AuthLoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AuthMapper authMapper;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public AuthLoginVO login(AuthLoginDTO loginDTO) {
        log.info("用户登录: {}", loginDTO.getUsername());
        String username = loginDTO.getUsername();
        String password = loginDTO.getPassword();

        User user = authMapper.getByUsername(username);

        //处理各种问题
        if (user == null) {
            log.info("用户不存在: {}", username);
            throw new RuntimeException("用户不存在");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.info("密码错误: {}", username);
            throw new RuntimeException("密码错误");
        }
        if(user.getStatus().equals("DISABLED")){
            log.info("用户被禁用: {}", username);
            throw new RuntimeException("用户被禁用");
        }
        String token = jwtUtils.createJWT(user.getUserId(), user.getRole());
        AuthLoginVO authLoginVO = AuthLoginVO.builder()
                .username(user.getUsername())
                .token(token) // 这里应该生成一个实际的token
                .userId(user.getUserId())
                .role(user.getRole())
                .build();
        return authLoginVO;
    }

    @Override
    public void register(AuthRegisterDTO registerDTO) {
        log.info("用户注册: {}", registerDTO.getUsername());
        // 实现注册逻辑
        User user = new User();
        BeanUtils.copyProperties(registerDTO, user);
        user.setUpdatedAt(LocalDateTime.now());
        user.setCreatedAt(LocalDateTime.now());
        authMapper.insert(user);
    }
}
