package com.github.acer20246.resourceflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户登录返回对象")
public class AuthLoginVO {
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "用户令牌")
    private String token;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "用户角色")
    private String role;
}
