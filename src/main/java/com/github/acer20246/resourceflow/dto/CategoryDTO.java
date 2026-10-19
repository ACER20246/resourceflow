package com.github.acer20246.resourceflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(description = "分类信息")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {
    @Schema(description = "分类名称", example = "学习资料")
    private String name;
    @Schema(description = "分类描述", example = "用于存放学习资料的分类")
    private String description;
}
