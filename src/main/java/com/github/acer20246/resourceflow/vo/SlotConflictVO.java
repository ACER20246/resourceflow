package com.github.acer20246.resourceflow.vo;

import lombok.Data;

import java.time.LocalTime;

/**
 * 时间重叠校验命中的冲突预约信息，用于生成可读的冲突提示。
 */
@Data
public class SlotConflictVO {
    private Long reservationId;
    private String status;
    private Long slotId;
    private Long resourceId;
    private String resourceName;
    private LocalTime startTime;
    private LocalTime endTime;
}