package com.github.acer20246.resourceflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.acer20246.resourceflow.common.enums.ReservationStatus;
import com.github.acer20246.resourceflow.common.exception.BusinessException;
import com.github.acer20246.resourceflow.dto.ReservationDTO;
import com.github.acer20246.resourceflow.entity.Reservation;
import com.github.acer20246.resourceflow.entity.Resource;
import com.github.acer20246.resourceflow.entity.ResourceSlot;
import com.github.acer20246.resourceflow.mapper.ReservationMapper;
import com.github.acer20246.resourceflow.mapper.ResourceMapper;
import com.github.acer20246.resourceflow.mapper.ResourceSlotMapper;
import com.github.acer20246.resourceflow.mapper.UserMapper;
import com.github.acer20246.resourceflow.vo.ReservationVO;
import com.github.acer20246.resourceflow.vo.SlotConflictVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationTxService {


    @Value("${resourceflow.reservation.check-in-grace-minutes:15}")
    private long checkInGraceMinutes;

    private final ReservationMapper reservationMapper;
    private final ResourceSlotMapper resourceSlotMapper;
    private final ResourceMapper resourceMapper;
    private final UserMapper userMapper;

    @Value("${resourceflow.reservation.initial-status:CONFIRMED}")
    private String initialStatus;

    /**
     * 写库：user 行锁 → 幂等复查 → 时段校验 → 时间重叠 → 原子扣减 → 落库。
     * 这些都是"必须在同一个事务里"的操作，一个都不能挪到外层。
     */
    @Transactional(rollbackFor = Exception.class)
    public ReservationVO create(Long userId, ReservationDTO dto){
        // 0. 校验初始状态配置是否合法
        ReservationStatus target = ReservationStatus.of(initialStatus);
        if (!target.isOccupying()) {
            throw new BusinessException("预约初始状态配置非法: " + initialStatus);
        }

        // 1. 锁定用户行：同一用户的并发预约在此串行化，否则时间重叠校验会被并发绕过
        if (userMapper.lockById(userId) == null) {
            throw new BusinessException("用户不存在");
        }
        // 2. 幂等：同一 requestId 直接返回已有预约
        Reservation duplicated = findByRequestId(userId, dto.getRequestId());
        if (duplicated != null) {
            log.info("幂等命中，requestId={}, reservationId={}", dto.getRequestId(), duplicated.getReservationId());
            return toVO(duplicated);
        }

        // 3. 时间段校验：以库中数据为准，不信任请求中的日期时间
        ResourceSlot slot = loadBookableSlot(dto.getResourceId(), dto.getSlotId());

        // 4. 时间重叠校验：同一用户同一天内，活跃预约的时间区间不能相交
        SlotConflictVO conflict = reservationMapper.selectUserOverlaps(
                userId,
                slot.getSlotDate(),
                slot.getStartTime(),
                slot.getEndTime(),
                null,
                ReservationStatus.occupyingNames());
        if (conflict != null) {
            throw new BusinessException("该时段与已有预约冲突：%s，请先取消原预约或选择其他时段"
                    .formatted(describeConflict(conflict)));
        }
        // 5. 原子扣减名额，失败说明已被抢完
        if (resourceSlotMapper.tryOccupy(slot.getSlotId()) == 0) {
            throw new BusinessException("该时段已约满，可加入候补");
        }
        // 6. 落库
        LocalDateTime now = LocalDateTime.now();
        Reservation reservation = new Reservation();
        reservation.setUserId(userId);
        reservation.setSlotId(slot.getSlotId());
        reservation.setRequestId(dto.getRequestId());
        reservation.setRemark(dto.getRemark());
        reservation.setStatus(target.name());
        reservation.setActiveKey(userId + ":" + slot.getSlotId());
        reservation.setCreatedAt(now);
        reservation.setUpdatedAt(now);
        try {
            reservationMapper.insert(reservation);
        } catch (DuplicateKeyException e) {
            log.warn("预约唯一键冲突，视为重复提交: userId={}, slotId={}", userId, slot.getSlotId());
            throw new BusinessException("该时段已有预约，请勿重复提交");
        }
        log.info("创建预约成功: reservationId={}, userId={}, slotId={}",
                reservation.getReservationId(), userId, slot.getSlotId());
        return toVO(reservation);
    }

    //  校验时间段是否可预约，返回数据库中真实的时段数据
    public ResourceSlot loadBookableSlot(Long resourceId, Long slotId) {
        ResourceSlot slot = resourceSlotMapper.selectById(slotId);
        if (slot == null) {
            throw new BusinessException("时间段不存在");
        }
        if (!Objects.equals(slot.getResourceId(), resourceId)) {
            throw new BusinessException("时间段与研修室不匹配");
        }
        if (!Objects.equals(slot.getStatus(), "AVAILABLE") && !Objects.equals(slot.getStatus(), "FULL")) {
            throw new BusinessException("该时间段不可预约");
        }
        if (slot.getSlotDate() == null || slot.getStartTime() == null || slot.getEndTime() == null) {
            throw new BusinessException("时间段数据异常");
        }
        if (!slot.getSlotDate().atTime(slot.getStartTime()).isAfter(LocalDateTime.now())) {
            throw new BusinessException("不能预约已开始的时间段");
        }
        return slot;
    }

    // 将冲突信息描述为字符串，供异常提示使用
    private String describeConflict(SlotConflictVO conflict) {
        String resourceName = conflict.getResourceName() == null ? "已有预约" : conflict.getResourceName();
        return "%s %s-%s".formatted(resourceName, conflict.getStartTime(), conflict.getEndTime());
    }
    private ReservationVO toVO(Reservation reservation) {
        ResourceSlot slot = reservation.getSlotId() == null
                ? null
                : resourceSlotMapper.selectById(reservation.getSlotId());
        Resource resource = (slot == null || slot.getResourceId() == null)
                ? null
                : resourceMapper.selectById(slot.getResourceId());
        LocalDateTime start = slot == null ? null : slot.getSlotDate().atTime(slot.getStartTime());
        return ReservationVO.builder()
                .reservationId(reservation.getReservationId())
                .userId(reservation.getUserId())
                .resourceId(slot == null ? null : slot.getResourceId())
                .resourceName(resource == null ? null : resource.getName())
                .slotId(reservation.getSlotId())
                .slotDate(slot == null ? null : slot.getSlotDate())
                .startTime(slot == null ? null : slot.getStartTime())
                .endTime(slot == null ? null : slot.getEndTime())
                .status(reservation.getStatus())
                .remark(reservation.getRemark())
                .createdAt(reservation.getCreatedAt())
                .cancelledAt(reservation.getCancelledAt())
                .completedAt(reservation.getCompletedAt())
                .checkedInAt(reservation.getCheckedInAt())
                .canCheckIn(canCheckIn(reservation,start))
                .checkinDeadline(start == null ? null : start.plusMinutes(checkInGraceMinutes))
                .build();
    }
    private boolean canCheckIn(Reservation reservation, LocalDateTime start) {
        if (start == null || reservation.getCheckedInAt() != null) {
            return false;
        }
        if (!ReservationStatus.CONFIRMED.name().equals(reservation.getStatus())) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(start) && !now.isAfter(start.plusMinutes(checkInGraceMinutes));
    }

    private Reservation findByRequestId(Long userId, String requestId) {
        return reservationMapper.selectOne(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, userId)
                .eq(Reservation::getRequestId, requestId)
                .last("LIMIT 1"));
    }
}
