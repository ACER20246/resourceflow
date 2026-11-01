package com.github.acer20246.resourceflow.task;

import com.github.acer20246.resourceflow.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 预约状态定时任务。
 *
 * <p>两个任务都用 {@code fixedDelay}（上一次执行完才开始计时），避免任务堆叠；
 * 每次执行循环到没有可处理数据为止，防止积压追不上。</p>
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class ReservationStatusTask {
    private final ReservationService reservationService;

    /**
     * 处理过期预约, 过期未签到的预约状态更新为已过期。
     */
    @Scheduled(fixedDelay = 1000 * 60) //1分钟执行一次
    public void expiredNoShow () {
        int total = 0;
        int handled = 0;
        while((handled = reservationService.expireUnCheckedInReservations()) > 0) {
            total += handled;
        }
        log.info("处理了 {} 个过期预约", total);
    }

    /**
     * 处理已结束预约, 已结束的预约状态更新为已完成。
     */
    @Scheduled(fixedDelay = 1000 * 60 ) //1分钟执行一次
    public void completeFinishedReservations () {
        int total = 0;
        int handled = 0;
        while((handled = reservationService.completeFinishedReservations()) > 0) {
            total += handled;
        }
        log.info("处理了 {} 个已结束预约", total);
    }
}
