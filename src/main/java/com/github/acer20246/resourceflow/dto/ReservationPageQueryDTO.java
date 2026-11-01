package com.github.acer20246.resourceflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 我的预约分页查询条件。
 *
 * <p>日期区间按预约创建时间过滤，格式为 yyyy-MM-dd。</p>
 */
@Schema(description = "我的预约分页查询条件")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationPageQueryDTO {
    @Schema(description = "预约状态，不传则查询全部")
    private String status;

    @Schema(description = "创建时间起（含），格式 yyyy-MM-dd")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @Schema(description = "创建时间止（含），格式 yyyy-MM-dd")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    private Integer page;
    private Integer pageSize;
}
