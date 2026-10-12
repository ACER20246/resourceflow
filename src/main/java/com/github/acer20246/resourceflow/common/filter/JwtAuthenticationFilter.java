package com.github.acer20246.resourceflow.common.filter;
import com.github.acer20246.resourceflow.common.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            log.debug("请求头 Authorization 为空，path={}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }
        try {
            Claims claims = jwtUtils.parseJWT(token);
            Long userId = Long.valueOf(claims.getSubject());
            String role = claims.get("role", String.class);
            if (role == null || role.isBlank()) {
                role = "USER";
            }
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userId
                        ,null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
                // Continue with the authentication process
            log.info("当前用户ID: {}, 角色: {}", userId, role);
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        } catch (Exception e) {
            log.warn("JWT 解析失败，请求路径：{}", request.getRequestURI());
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }
}
