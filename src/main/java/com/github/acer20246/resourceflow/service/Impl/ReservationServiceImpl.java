package com.github.acer20246.resourceflow.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.acer20246.resourceflow.common.enums.ReservationStatus;
import com.github.acer20246.resourceflow.common.exception.BusinessException;
import com.github.acer20246.resourceflow.common.redis.ReservationKeys;
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
import com.github.acer20246.resourceflow.service.ReservationGuard;
import com.github.acer20246.resourceflow.service.ReservationService;
import com.github.acer20246.resourceflow.service.ReservationTxService;
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
    private final ReservationTxService reservationTxService;
    private final ReservationGuard guard;

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
    public ReservationVO createReservation(Long userId, ReservationDTO dto) {
        validateCreateRequest(userId, dto);
        // 时间段校验：以库中数据为准，不信任请求中的日期时间
        ResourceSlot slot = reservationTxService.loadBookableSlot(dto.getResourceId(), dto.getSlotId());
        // 构建时段唯一标识
        String slotKey = ReservationKeys.slot(slot);
        // 幂等键：userId + requestId，覆盖当天重试窗口
        String idemKey = ReservationKeys.idempotent(userId, dto.getRequestId());
        //owner token
        String token = ReservationGuard.token(userId, dto.getRequestId());

        // 3. 幂等校验：若已处理过则直接返回结果，若正在处理中则抛出异常
        ReservationGuard.IdemOutcome idemOutcome = guard.tryAcquire(idemKey, token);
        switch (idemOutcome.state()) {
            case DONE -> {
                log.info("幂等键已完成，直接返回结果: userId={}, requestId={}", userId, dto.getRequestId());
                return toVO(requireReservation(idemOutcome.reservationId()));
            }
            case IN_PROGRESS -> {
                log.warn("幂等键正在处理中，请勿重复提交: userId={}, requestId={}", userId, dto.getRequestId());
                throw new BusinessException("请求正在处理中，请勿重复提交");
            }
            case DEGRADED -> {
                log.warn("幂等键获取失败，降级处理: userId={}, requestId={}", userId, dto.getRequestId());
            }
            case ACQUIRED -> {
                log.info("幂等键获取成功，继续处理: userId={}, requestId={}", userId, dto.getRequestId());
            }
        }
        ReservationGuard.OccupyResult occupyResult = guard.tryOccupy(slotKey,slot.getBookingCapacity(),token);
        if(occupyResult==ReservationGuard.OccupyResult.FULL) {
            guard.clear(idemKey,token);
            throw new BusinessException("该时段已约满，可加入候补");
        }
        if(occupyResult==ReservationGuard.OccupyResult.ALREADY_OCCUPIED) {
            Reservation existing = findByRequestId(userId, dto.getRequestId());
            if(existing != null) {
                guard.markDone(idemKey,existing.getReservationId());
                return toVO(existing);
            }
        }
        try{
            ReservationVO vo = reservationTxService.create(userId, dto);
            guard.markDone(idemKey,vo.getReservationId());
            return vo;
        }catch (RuntimeException e) {
            guard.release(slotKey,token);
            guard.clear(idemKey,token);
            throw e;
        }
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
    @Transactional(rollbackFor = Exception.class)
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
