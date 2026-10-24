package com.github.acer20246.resourceflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.acer20246.resourceflow.entity.Reservation;
import com.github.acer20246.resourceflow.vo.SlotConflictVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {

    /**
     * 查询同一用户在指定日期内与给定时间区间重叠的活跃预约。
     * <p>重叠判定使用半开区间 {@code [start, end)}：
     * {@code existing.start < new.end AND existing.end > new.start}，
     * 因此 09:00-10:00 与 10:00-11:00 不会被判定为冲突。</p>
     *
     * @param userId               用户 ID
     * @param slotDate             预约日期
     * @param startTime            新预约开始时间
     * @param endTime              新预约结束时间
     * @param excludeReservationId 需要排除的预约 ID，可为空
     * @param occupyingStatuses    参与冲突判断的状态集合，见 ReservationStatus.occupyingNames()
     * @return 冲突预约，无冲突返回 null
     */
    SlotConflictVO selectUserOverlaps(@Param("userId") Long userId,
                                      @Param("slotDate") LocalDate slotDate,
                                      @Param("startTime") LocalTime startTime,
                                      @Param("endTime") LocalTime endTime,
                                      @Param("excludeReservationId") Long excludeReservationId,
                                      @Param("occupyingStatuses") Collection<String> occupyingStatuses);

    /**
     * 按状态机流转预约状态，使用 compare-and-set 保证并发安全与幂等。
     *
     * @param reservationId 预约 ID
     * @param expected      期望的当前状态，不匹配则不更新
     * @param target        目标状态
     * @param now           当前时间
     * @return 影响行数，0 表示状态已被并发修改
     */
    int transit(@Param("reservationId") Long reservationId,
                @Param("expected") String expected,
                @Param("target") String target,
                @Param("now") LocalDateTime now);

    /**
     * 查询时间段已经结束的预约，供定时任务批量置为已完成。
     * @param now   当前时间
     * @param limit 单批上限，避免长事务
     * @return 待完成的预约
     */
    List<Reservation> selectFinishedConfirmed(@Param("now") LocalDateTime now, @Param("limit") int limit);
}
