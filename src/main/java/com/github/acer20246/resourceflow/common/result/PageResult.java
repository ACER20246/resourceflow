package com.github.acer20246.resourceflow.common.result;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "分页结果")
public record PageResult<T>(
        @Schema(description = "当前页数据")
        List<T> records,

        @Schema(description = "页码")
        long page,

        @Schema(description = "每页条数")
        long pageSize,

        @Schema(description = "总条数")
        long total
) {
}