package com.github.acer20246.resourceflow.common.utils;

import com.github.acer20246.resourceflow.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户工具类。
 *
 * <p>{@code JwtAuthenticationFilter} 把用户 ID 作为 principal 放入 SecurityContext，
 * 这里统一读取，避免每个 Controller 重复写强转逻辑。</p>
 */
public final class SecurityUtils {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户 ID。
     *
     * @return 用户 ID
     * @throws BusinessException 未登录或认证信息格式不正确
     */
    public static Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();// 获取当前认证信息
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        throw new BusinessException("未登录或登录状态已失效");
    }

    /**
     * 当前用户是否为管理员。
     */
    public static boolean isAdmin() {
        // 获取当前认证信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> ROLE_ADMIN.equals(authority.getAuthority()));
    }
}
