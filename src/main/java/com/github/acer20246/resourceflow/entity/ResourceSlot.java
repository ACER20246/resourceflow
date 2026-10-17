package com.github.acer20246.resourceflow.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
/*
 记录某个研修室在某个日期的具体可预约时间段，
 例如“2026-10-08 09:00 至 10:00”。它是系统控制预约库存和防超卖的核心表。
 */
public class ResourceSlot {
    private Long slotId;
    //时间段Id
    private Long resourceId;
    //研修室ID
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Date slotDate;
    //预约日期
    private Integer bookingCapacity;
    //该时间段允许预约的数量
    private Integer reservedCount;
    //当前已确定预约数量
    private Integer version;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
