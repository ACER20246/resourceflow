package com.github.acer20246.resourceflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Schema(description = "用户分页查询DTO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPageQueryDTO {
    @Schema(description = "姓名")
    private String username;

    @Schema(description = "学号")
    private String studentNumber;


    @Schema(description = "页码")
    private Integer pageNum;

    @Schema(description = "每页数量")
    private Integer pageSize;
}
