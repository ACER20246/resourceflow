package com.github.acer20246.resourceflow.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.acer20246.resourceflow.common.enums.ReservationStatus;
import com.github.acer20246.resourceflow.common.exception.BusinessException;
import com.github.acer20246.resourceflow.common.result.PageResult;
import com.github.acer20246.resourceflow.dto.ReservationDTO;
import com.github.acer20246.resourceflow.dto.ReservationPageQueryDTO;
import com.github.acer20246.resourceflow.entity.Reservation;
import com.github.acer20246.resourceflow.entity.Resource;
import com.github.acer20246.resourceflow.entity.ResourceSlot;
import com.github.acer20246.resourceflow.mapper.ReservationMapper;
import com.github.acer20246.resourceflow.mapper.ResourceMapper;
import com.github.acer20246.resourceflow.mapper.ResourceSlotMapper;
import com.github.acer20246.resourceflow.mapper.UserMapper;
import com.github.acer20246.resourceflow.service.ReservationService;
import com.github.acer20246.resourceflow.vo.ReservationVO;
import com.github.acer20246.resourceflow.vo.SlotConflictVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 预约服务实现。
 *
 * <p>创建预约按「幂等校验 → 锁定用户 → 时段校验 → 时间重叠校验 → 原子扣减名额 → 落库」
 * 的顺序执行，库存扣减依赖数据库条件更新保证不超卖；状态变更统一走
 * {@code ReservationStatus} 状态机，取消与失效会回补名额。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    /** 定时任务单批处理上限，避免长事务。 */
    private static final int TASK_BATCH_SIZE = 200;
    @Value("${resourceflow.reservation.check-in-grace-minutes:15}")
    private long checkInGraceMinutes;

    private final ReservationMapper reservationMapper;
    private final ResourceSlotMapper resourceSlotMapper;
    private final ResourceMapper resourceMapper;
    private final UserMapper userMapper;

    /** 新建预约的初始状态，默认直接确认；如需人工审核可配置为 PENDING。 */
    @Value("${resourceflow.reservation.initial-status:CONFIRMED}")
    private String initialStatus;

    /** PENDING 状态超时时间（分钟），超过后自动失效并回补名额。 */
    @Value("${resourceflow.reservation.pending-timeout-minutes:15}")
    private long pendingTimeoutMinutes;

    @Override
    public ReservationVO getReservationById(Long reservationId, Long operatorId, boolean admin) {
        Reservation reservation = requireReservation(reservationId);
        if (!admin && !Objects.equals(reservation.getUserId(), operatorId)) {
            throw new BusinessException("无权查看他人的预约");
        }
        return toVO(reservation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationVO createReservation(Long userId, ReservationDTO dto) {
        validateCreateRequest(userId, dto);

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

    @Override
    public PageResult<ReservationVO> getMyReservations(Long userId, ReservationPageQueryDTO queryDTO) {
        long pageNum = queryDTO.getPage() == null || queryDTO.getPage() < 1 ? 1 : queryDTO.getPage();
        long pageSize = queryDTO.getPageSize() == null || queryDTO.getPageSize() < 1 ? 10 : queryDTO.getPageSize();
        Page<Reservation> page = new Page<>(pageNum, pageSize);

        LocalDateTime from = queryDTO.getStartDate() == null ? null : queryDTO.getStartDate().atStartOfDay();
        LocalDateTime to = queryDTO.getEndDate() == null ? null : queryDTO.getEndDate().plusDays(1).atStartOfDay();

        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getUserId, userId)
                .eq(queryDTO.getStatus() != null && !queryDTO.getStatus().isBlank(),
                        Reservation::getStatus, queryDTO.getStatus())
                .ge(from != null, Reservation::getCreatedAt, from)
                .lt(to != null, Reservation::getCreatedAt, to)
                .orderByDesc(Reservation::getCreatedAt);

        reservationMapper.selectPage(page, wrapper);
        List<ReservationVO> records = page.getRecords().stream()
                .map(this::toVO)
                .toList();
        return new PageResult<>(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationVO cancelReservation(Long reservationId, Long operatorId, boolean admin) {
        Reservation reservation = requireReservation(reservationId);
        if (!admin && !Objects.equals(reservation.getUserId(), operatorId)) {
            throw new BusinessException("无权操作他人的预约");
        }
        applyTransition(reservation, ReservationStatus.CANCELLED);
        return toVO(requireReservation(reservationId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationVO changeStatus(Long reservationId, String targetStatus, Long operatorId, boolean admin) {
        Reservation reservation = requireReservation(reservationId);
        if (!admin && !Objects.equals(reservation.getUserId(), operatorId)) {
            throw new BusinessException("无权操作他人的预约");
        }
        applyTransition(reservation, ReservationStatus.of(targetStatus));
        return toVO(requireReservation(reservationId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int completeFinishedReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> finished = reservationMapper.selectFinishedConfirmed(now, TASK_BATCH_SIZE);
        int affected = 0;
        for (Reservation reservation : finished) {
            if (transit(reservation, ReservationStatus.COMPLETED)) {
                affected++;
            }
        }
        return affected;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean checkInReservation(Long reservationId, Long operatorId,boolean admin) {
        Reservation reservation = requireReservation(reservationId);
        if (!Objects.equals(reservation.getStatus(), ReservationStatus.CONFIRMED.name())) {
            throw new BusinessException("预约状态不允许签到");
        }
        if(!Objects.equals(reservation.getUserId(),operatorId)&&!admin) {
            throw new BusinessException("无权操作他人的预约");
        }
        if(reservation.getCheckedInAt() != null) {
            throw new BusinessException("预约已签到");
        }
        ResourceSlot slot = resourceSlotMapper.selectById(reservation.getSlotId());
        if(slot == null) {
            throw new BusinessException("预约对应的时间段不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime slotStart = slot.getSlotDate().atTime(slot.getStartTime());
        if(now.isBefore(slotStart)) {
            throw new BusinessException("预约尚未开始，无法签到");
        }
        if(now.isAfter(slotStart.plusMinutes(checkInGraceMinutes))) {
            throw new BusinessException("签到已过宽限期，无法签到");
        }
        if(reservationMapper.checkInReservation(reservationId) == 0) {
            throw new BusinessException("预约状态已被并发修改，请刷新后重试");
        }
        log.info("预约签到成功: reservationId={}, userId={}", reservationId, operatorId);
        return true;
    }
    /**
     * 校验当前状态并执行流转，成功后按目标状态决定是否回补名额。
     */
    private void applyTransition(Reservation reservation, ReservationStatus target) {
        ReservationStatus current = ReservationStatus.of(reservation.getStatus());
        if (!current.canTransitTo(target)) {
            throw new BusinessException("预约状态不允许从 %s 流转到 %s".formatted(current.name(), target.name()));
        }
        if(!transit(reservation, target)) {
            throw new BusinessException("预约状态已被并发修改，请刷新后重试");
        }
    }

    /**
     * 条件更新状态，并处理名额回补。返回 false 表示状态已被并发修改。
     */
    private boolean transit(Reservation reservation, ReservationStatus target) {
        int rows = reservationMapper.transit(
                reservation.getReservationId(),
                reservation.getStatus(),
                target.name(),
                LocalDateTime.now());
        if (rows == 0) {
            return false;
        }
        if (target.releasesCapacity()) {
            resourceSlotMapper.release(reservation.getSlotId());
        }
        return true;
    }

    // 6. 校验时间段是否可预约，返回数据库中真实的时段数据
    private ResourceSlot loadBookableSlot(Long resourceId, Long slotId) {
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

    private Reservation findByRequestId(Long userId, String requestId) {
        return reservationMapper.selectOne(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, userId)
                .eq(Reservation::getRequestId, requestId)
                .last("LIMIT 1"));
    }
    // 获取预约实体，若不存在则抛出异常
    private Reservation requireReservation(Long reservationId) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new BusinessException("预约不存在");
        }
        return reservation;
    }

    // 将冲突信息描述为字符串，供异常提示使用
    private String describeConflict(SlotConflictVO conflict) {
        String resourceName = conflict.getResourceName() == null ? "已有预约" : conflict.getResourceName();
        return "%s %s-%s".formatted(resourceName, conflict.getStartTime(), conflict.getEndTime());
    }

    // 将 Reservation 实体转换为 VO，附带资源与时段信息
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

    // 校验创建预约请求的合法性，抛出 BusinessException 表示请求不合法
    private void validateCreateRequest(Long userId, ReservationDTO dto) {
        if (userId == null) {
            throw new BusinessException("预约用户不能为空");
        }
        if (dto == null) {
            throw new BusinessException("预约请求不能为空");
        }
        if (dto.getResourceId() == null) {
            throw new BusinessException("研修室 ID 不能为空");
        }
        if (dto.getSlotId() == null) {
            throw new BusinessException("时间段 ID 不能为空");
        }
        if (dto.getRequestId() == null || dto.getRequestId().isBlank()) {
            throw new BusinessException("requestId 不能为空");
        }
    }

    // 未签到
    @Override
    public int expireUnCheckedInReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> expired = reservationMapper.selectExpiredCheckIn(now, TASK_BATCH_SIZE, checkInGraceMinutes);
        int affected = 0;
        for (Reservation reservation : expired) {
            if (transit(reservation, ReservationStatus.EXPIRED)) {
                affected++;
            }
        }
        return affected;
    }
}
