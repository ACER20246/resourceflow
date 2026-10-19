package com.github.acer20246.resourceflow.service.Impl;

import com.github.acer20246.resourceflow.common.exception.BusinessException;
import com.github.acer20246.resourceflow.common.utils.JwtUtils;
import com.github.acer20246.resourceflow.dto.AuthLoginDTO;
import com.github.acer20246.resourceflow.dto.AuthRegisterDTO;
import com.github.acer20246.resourceflow.entity.User;
import com.github.acer20246.resourceflow.mapper.AuthMapper;
import com.github.acer20246.resourceflow.vo.AuthLoginVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AuthServiceImpl} 单元测试：覆盖登录成功/失败与注册成功/重复等场景。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthMapper authMapper;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private User buildUser() {
        User user = new User();
        user.setUserId(10001L);
        user.setUsername("zhangsan");
        user.setPassword("hashed-password");
        user.setPhone("13800138000");
        user.setRole("USER");
        user.setStatus("ENABLED");
        return user;
    }

    @Test
    @DisplayName("登录成功：返回 token、用户 ID 和角色")
    void loginSuccess() {
        // arrange
        User user = buildUser();
        when(authMapper.getByUsername("zhangsan")).thenReturn(user);
        when(passwordEncoder.matches("Password123", "hashed-password")).thenReturn(true);
        when(jwtUtils.createJWT(10001L, "USER")).thenReturn("token-abc");

        // act
        AuthLoginVO vo = authService.login(new AuthLoginDTO("zhangsan", "Password123"));

        // assert
        assertThat(vo.getToken()).isEqualTo("token-abc");
        assertThat(vo.getUserId()).isEqualTo(10001L);
        assertThat(vo.getUsername()).isEqualTo("zhangsan");
        assertThat(vo.getRole()).isEqualTo("USER");
    }

    @Test
    @DisplayName("登录失败：用户不存在时提示用户名或密码错误")
    void loginWithUnknownUsername() {
        // arrange
        when(authMapper.getByUsername("nobody")).thenReturn(null);

        // act & assert
        assertThatThrownBy(() -> authService.login(new AuthLoginDTO("nobody", "Password123")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名或密码错误");
    }

    @Test
    @DisplayName("登录失败：密码错误时提示用户名或密码错误")
    void loginWithWrongPassword() {
        // arrange
        when(authMapper.getByUsername("zhangsan")).thenReturn(buildUser());
        when(passwordEncoder.matches("WrongPassword", "hashed-password")).thenReturn(false);

        // act & assert
        assertThatThrownBy(() -> authService.login(new AuthLoginDTO("zhangsan", "WrongPassword")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名或密码错误");
    }

    @Test
    @DisplayName("登录失败：被禁用用户提示用户已被禁用")
    void loginWithDisabledUser() {
        // arrange
        User user = buildUser();
        user.setStatus("DISABLED");
        when(authMapper.getByUsername("zhangsan")).thenReturn(user);
        when(passwordEncoder.matches("Password123", "hashed-password")).thenReturn(true);

        // act & assert
        assertThatThrownBy(() -> authService.login(new AuthLoginDTO("zhangsan", "Password123")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户已被禁用");
    }

    @Test
    @DisplayName("注册成功：密码加密、默认角色 USER、状态 ENABLED")
    void registerSuccess() {
        // arrange
        AuthRegisterDTO dto = AuthRegisterDTO.builder()
                .username("zhangsan")
                .password("Password123")
                .phone("13800138000")
                .studentNumber("2026001")
                .build();
        when(authMapper.getByUsername("zhangsan")).thenReturn(null);
        when(passwordEncoder.encode("Password123")).thenReturn("hashed-password");

        // act
        authService.register(dto);

        // assert：校验落库对象
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(authMapper).insert(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("zhangsan");
        assertThat(saved.getPassword()).isEqualTo("hashed-password");
        assertThat(saved.getPhone()).isEqualTo("13800138000");
        assertThat(saved.getStudentNumber()).isEqualTo("2026001");
        assertThat(saved.getRole()).isEqualTo("USER");
        assertThat(saved.getStatus()).isEqualTo("ENABLED");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
    }

    @Test
    @DisplayName("注册失败：用户名已存在时抛出业务异常且不落库")
    void registerWithDuplicateUsername() {
        // arrange
        AuthRegisterDTO dto = AuthRegisterDTO.builder()
                .username("zhangsan")
                .password("Password123")
                .build();
        when(authMapper.getByUsername("zhangsan")).thenReturn(buildUser());

        // act & assert
        assertThatThrownBy(() -> authService.register(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名已存在");
        verify(authMapper, never()).insert(any(User.class));
    }
}