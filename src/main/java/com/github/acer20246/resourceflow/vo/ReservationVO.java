package com.github.acer20246.resourceflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 预约详情视图。
 *
 * <p>把时间段信息（日期、开始与结束时间）连同预约状态一起平铺返回，
 * 前端不需要再单独查询时间段。</p>
 */
@Schema(description = "预约详情")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationVO {
    private Long reservationId;
    private Long userId;
    private Long resourceId;//研修室 ID
    private String resourceName;
    private Long slotId;
    private LocalDate slotDate;
    private LocalTime startTime;
    private LocalTime endTime;
    @Schema(description = "预约状态：PENDING/CONFIRMED/CANCELLED/COMPLETED/EXPIRED/NO_SHOW")
    private String status;
    private String remark;
    private String cancelReason;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime completedAt;
    private LocalDateTime expiredAt;
    private LocalDateTime checkedInAt;
    private LocalDateTime checkinDeadline;
    private boolean canCheckIn;
}
