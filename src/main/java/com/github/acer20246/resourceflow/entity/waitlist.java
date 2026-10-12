package com.github.acer20246.resourceflow.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
/*
  记录研修室时间段已满时，用户按照先来先服务原则加入的候补队列
 */
public class waitlist {
    private Long waitlistId;
    private Long userId;
    private Long slotId;
    //候补时间段
    private String requestId;

    private String activeKey;
    // 活跃键

    private Integer queueNo;
    // 该时间段内的候补序号，从 1 开始

    private String status;
    //描述候补情况
    private LocalDateTime createdAt;
    //加入候补时间

    private LocalDateTime promotedAt;
    // 候补被提升时间
    private LocalDateTime updatedAt;
    // 更新时间
    private LocalDateTime cancelledAt;
    //取消时间
}
