package com.github.acer20246.resourceflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Resource {
    @TableId(value = "resource_id",type = IdType.AUTO)
    private Long resourceId;
    private String name;
    private String description;
    private Long categoryId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String location;
    private Integer capacity;
}
