package com.github.acer20246.resourceflow.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {
    @TableId(value = "reservation_id")
    private Long reservationId;
    private Long userId;
    private Long slotId;
    private Long sourceWaitlistId;
    private String requestId;
    private String activeKey;
    private String remark;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiredAt;
    private LocalDateTime checkedInAt;
}
