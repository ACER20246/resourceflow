package com.github.acer20246.resourceflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "资源信息")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResourceVO {
    private Long resourceId;
    private String name;
    private String categoryName;
    private String description;
    private Long categoryId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String location;
    private Integer capacity;
}
