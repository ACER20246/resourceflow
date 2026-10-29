package com.github.acer20246.resourceflow.common.utils;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link JwtUtils} 单元测试：覆盖令牌生成、解析以及各类异常令牌场景。
 */
class JwtUtilsTest {

    /**
     * HMAC-SHA256 要求密钥长度不低于 32 字节，这里使用 64 位字符密钥。
     */
    private static final String SECRET_KEY = "resourceflow-test-secret-key-0123456789abcdef0123456789abcdef";

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMillis", 3600_000L);
    }

    @Test
    @DisplayName("生成的 token 可以解析出用户 ID 和角色")
    void createJwtThenParseSuccessfully() {
        // arrange & act
        String token = jwtUtils.createJWT(10001L, "USER");

        // assert
        assertThat(token).isNotBlank();
        Claims claims = jwtUtils.parseJWT(token);
        assertThat(claims.getSubject()).isEqualTo("10001");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
    }

    @Test
    @DisplayName("过期 token 解析时抛出“token 已过期”")
    void parseExpiredToken() {
        // arrange：过期时间设为负数，生成一个已过期的 token
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMillis", -10_000L);
        String token = jwtUtils.createJWT(10001L, "ADMIN");

        // act & assert
        assertThatThrownBy(() -> jwtUtils.parseJWT(token))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("token 已过期");
    }

    @Test
    @DisplayName("格式错误的 token 解析时抛出“token 格式错误”")
    void parseMalformedToken() {
        // act & assert
        assertThatThrownBy(() -> jwtUtils.parseJWT("not-a-jwt-token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("token 格式错误");
    }

    @Test
    @DisplayName("空 token 解析时抛出“token 为空”")
    void parseBlankToken() {
        // act & assert
        assertThatThrownBy(() -> jwtUtils.parseJWT(""))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("token 为空");
    }
}