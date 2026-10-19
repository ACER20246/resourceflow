package com.github.acer20246.resourceflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Schema(description = "用户DTO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    @Schema(description = "用户名")
    private String username;

    @Schema(description = "学号")
    private String studentNumber;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;
}
