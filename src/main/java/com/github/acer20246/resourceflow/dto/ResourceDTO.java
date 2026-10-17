package com.github.acer20246.resourceflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Schema(description = "资源信息")
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDTO {
    private String name;
    private String description;
    private Long categoryId;
    private String location;
    private Integer capacity;
}
