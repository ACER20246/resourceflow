package com.github.acer20246.resourceflow.dto;

import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

/**
 * 时间段分页查询条件。
 *
 * <p>所有字段均为可选，按「指定哪个就过滤哪个」的方式动态拼接查询条件。
 * 时间字段通过 {@code @DateTimeFormat} 声明入参格式，避免不同来源的格式差异。</p>
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ResourceSlotPageDTO {
    /** 研修室 ID，不传则查询全部研修室 */
    private Long resourceId;
    /** 开始时间下限（含），格式 HH:mm:ss */
    @DateTimeFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;
    /** 结束时间上限（含），格式 HH:mm:ss */
    @DateTimeFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;
    /** 预约日期，格式 yyyy-MM-dd */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate slotDate;
    /** 页码，从 1 开始 */
    private Integer page;
    /** 每页记录数 */
    private Integer pageSize;
    /** 时间段状态：AVAILABLE / FULL / DISABLED / EXPIRED */
    private String status;

    /**
     * 校验时间区间是否合法，供 Bean Validation 使用。
     *
     * <p>任一端为空时不校验，只有两端都传了才要求开始时间不晚于结束时间。</p>
     *
     * @return 区间合法或无法判断时返回 true
     */
    @AssertTrue(message = "开始时间不能晚于结束时间")
    public boolean isTimeRangeValid() {
        if (startTime == null || endTime == null) {
            return true;
        }
        return !startTime.isAfter(endTime);
    }
}
