package com.github.acer20246.resourceflow.common.utils;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtUtils {

    @Value("${resourceflow.jwt.secret-key}")
    private String secretKey;

    @Value("${resourceflow.jwt.jwtExpirationMillis}")
    private long jwtExpirationMillis;
    /**
     * 生成jwt
     * @return
     */
    public String createJWT(Long userId,String role) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        long expMills = System.currentTimeMillis() + jwtExpirationMillis;
        return Jwts.builder()
                .signWith(key)
                .subject(String.valueOf(userId))
                .claim("role", role)
                .expiration(new java.util.Date(expMills))
                .compact();
    }

    /**
     * 解析令牌
     * @param token
     * @return
     */
    public Claims parseJWT(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("token 已过期", e);
        } catch (SignatureException e) {
            throw new RuntimeException("签名校验失败", e);
        } catch (MalformedJwtException e) {
            throw new RuntimeException("token 格式错误", e);
        } catch (UnsupportedJwtException e) {
            throw new RuntimeException("不支持的 token", e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("token 为空", e);
        }
    }
}
