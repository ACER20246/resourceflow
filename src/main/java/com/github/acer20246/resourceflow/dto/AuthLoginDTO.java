package com.github.acer20246.resourceflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(description = "用户登录DTO")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthLoginDTO {

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "密码")
    private String password;
}
