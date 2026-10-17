package com.github.acer20246.resourceflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(description = "资源分页查询条件")
@NoArgsConstructor
@AllArgsConstructor
public class ResourcePageQueryDTO {
    @Schema(description = "资源名称，模糊匹配")
    private String name;

    @Schema(description = "分类 ID")
    private Long categoryId;

    @Schema(description = "状态：ENABLED 或 DISABLED")
    private String status;

    @Schema(description = "页码，从 1 开始")
    private Long page = 1L;

    @Schema(description = "每页条数，最大 100")
    private Long pageSize = 20L;
}