package com.github.acer20246.resourceflow.service.Impl;

import com.github.acer20246.resourceflow.common.exception.BusinessException;
import com.github.acer20246.resourceflow.dto.ReservationDTO;
import com.github.acer20246.resourceflow.entity.Reservation;
import com.github.acer20246.resourceflow.entity.Resource;
import com.github.acer20246.resourceflow.entity.ResourceSlot;
import com.github.acer20246.resourceflow.mapper.ReservationMapper;
import com.github.acer20246.resourceflow.mapper.ResourceMapper;
import com.github.acer20246.resourceflow.mapper.ResourceSlotMapper;
import com.github.acer20246.resourceflow.service.ReservationGuard;
import com.github.acer20246.resourceflow.service.ReservationTxService;
import com.github.acer20246.resourceflow.vo.ReservationVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ReservationServiceImpl} 单元测试。
 *
 * <p>覆盖两层职责：外层编排（幂等占位 → 时段占位 → 事务写入 → 回写 / 补偿）与
 * 状态机相关操作（取消、状态流转、签到、定时任务）。事务内部的字段级校验由
 * {@code ReservationTxService} 与并发测试覆盖。</p>
 */
@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    private static final Long USER_ID = 10001L;
    private static final Long OTHER_USER_ID = 10002L;
    private static final Long RESOURCE_ID = 1001L;
    private static final Long SLOT_ID = 90001L;
    private static final Long RESERVATION_ID = 50001L;

    @Mock
    private ReservationMapper reservationMapper;

    @Mock
    private ResourceSlotMapper resourceSlotMapper;

    @Mock
    private ResourceMapper resourceMapper;

    @Mock
    private ReservationTxService reservationTxService;

    @Mock
    private ReservationGuard guard;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reservationService, "checkInGraceMinutes", 15L);
    }

    // ── 创建预约：外层编排 ────────────────────────────────────────

    @Test
    @DisplayName("创建预约：幂等键已是 DONE，直接返回原预约且不再占位")
    void createWhenIdempotentDone() {
        when(reservationTxService.loadBookableSlot(RESOURCE_ID, SLOT_ID)).thenReturn(buildSlot());
        when(guard.tryAcquire(anyString(), anyString()))
                .thenReturn(new ReservationGuard.IdemOutcome(ReservationGuard.IdemState.DONE, RESERVATION_ID));
        when(reservationMapper.selectById(RESERVATION_ID)).thenReturn(buildReservation("CONFIRMED"));
        stubToVo();

        ReservationVO result = reservationService.createReservation(USER_ID, buildDTO());

        assertThat(result.getReservationId()).isEqualTo(RESERVATION_ID);
        verify(guard, never()).tryOccupy(anyString(), anyInt(), anyString());
        verify(reservationTxService, never()).create(anyLong(), any());
    }

    @Test
    @DisplayName("创建预约：同一请求正在处理中，提示稍后重试")
    void createWhenIdempotentInProgress() {
        when(reservationTxService.loadBookableSlot(RESOURCE_ID, SLOT_ID)).thenReturn(buildSlot());
        when(guard.tryAcquire(anyString(), anyString()))
                .thenReturn(new ReservationGuard.IdemOutcome(ReservationGuard.IdemState.IN_PROGRESS, null));

        assertThatThrownBy(() -> reservationService.createReservation(USER_ID, buildDTO()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("正在处理中");

        verify(guard, never()).tryOccupy(anyString(), anyInt(), anyString());
        verify(reservationTxService, never()).create(anyLong(), any());
    }

    @Test
    @DisplayName("创建预约：闸门判定已满时清理幂等键，否则用户重试永远看到“处理中”")
    void createWhenGateReportsFull() {
        when(reservationTxService.loadBookableSlot(RESOURCE_ID, SLOT_ID)).thenReturn(buildSlot());
        when(guard.tryAcquire(anyString(), anyString()))
                .thenReturn(new ReservationGuard.IdemOutcome(ReservationGuard.IdemState.ACQUIRED, null));
        when(guard.tryOccupy(anyString(), anyInt(), anyString()))
                .thenReturn(ReservationGuard.OccupyResult.FULL);

        assertThatThrownBy(() -> reservationService.createReservation(USER_ID, buildDTO()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已约满");

        verify(guard).clear(anyString(), anyString());
        verify(reservationTxService, never()).create(anyLong(), any());
    }

    @Test
    @DisplayName("创建预约：成功后回写幂等结果，占位交给 TTL")
    void createSuccess() {
        ReservationVO created = ReservationVO.builder().reservationId(RESERVATION_ID).build();
        when(reservationTxService.loadBookableSlot(RESOURCE_ID, SLOT_ID)).thenReturn(buildSlot());
        when(guard.tryAcquire(anyString(), anyString()))
                .thenReturn(new ReservationGuard.IdemOutcome(ReservationGuard.IdemState.ACQUIRED, null));
        when(guard.tryOccupy(anyString(), anyInt(), anyString()))
                .thenReturn(ReservationGuard.OccupyResult.OK);
        when(reservationTxService.create(eq(USER_ID), any(ReservationDTO.class))).thenReturn(created);

        ReservationVO result = reservationService.createReservation(USER_ID, buildDTO());

        assertThat(result).isSameAs(created);
        // 容量取自数据库的 booking_capacity，不是请求参数
        verify(guard).tryOccupy(anyString(), eq(1), anyString());
        verify(guard).markDone(anyString(), eq(RESERVATION_ID));
        verify(guard, never()).release(anyString(), anyString());
    }

    @Test
    @DisplayName("创建预约：事务抛异常时释放占位并清理幂等键，异常继续上抛")
    void createWhenTxFailsCompensates() {
        when(reservationTxService.loadBookableSlot(RESOURCE_ID, SLOT_ID)).thenReturn(buildSlot());
        when(guard.tryAcquire(anyString(), anyString()))
                .thenReturn(new ReservationGuard.IdemOutcome(ReservationGuard.IdemState.ACQUIRED, null));
        when(guard.tryOccupy(anyString(), anyInt(), anyString()))
                .thenReturn(ReservationGuard.OccupyResult.OK);
        when(reservationTxService.create(eq(USER_ID), any(ReservationDTO.class)))
                .thenThrow(new BusinessException("该时段已约满，可加入候补"));

        assertThatThrownBy(() -> reservationService.createReservation(USER_ID, buildDTO()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已约满");

        verify(guard).release(anyString(), anyString());
        verify(guard).clear(anyString(), anyString());
        verify(guard, never()).markDone(anyString(), any());
    }

    @Test
    @DisplayName("创建预约：Redis 降级时跳过闸门，继续走数据库")
    void createWhenRedisDegraded() {
        ReservationVO created = ReservationVO.builder().reservationId(RESERVATION_ID).build();
        when(reservationTxService.loadBookableSlot(RESOURCE_ID, SLOT_ID)).thenReturn(buildSlot());
        when(guard.tryAcquire(anyString(), anyString()))
                .thenReturn(new ReservationGuard.IdemOutcome(ReservationGuard.IdemState.DEGRADED, null));
        when(guard.tryOccupy(anyString(), anyInt(), anyString()))
                .thenReturn(ReservationGuard.OccupyResult.DEGRADED);
        when(reservationTxService.create(eq(USER_ID), any(ReservationDTO.class))).thenReturn(created);

        ReservationVO result = reservationService.createReservation(USER_ID, buildDTO());

        assertThat(result.getReservationId()).isEqualTo(RESERVATION_ID);
    }

    // ── 取消预约 ─────────────────────────────────────────────────

    @Test
    @DisplayName("取消预约：CONFIRMED 流转为 CANCELLED 并回补名额")
    void cancelConfirmedReleasesCapacity() {
        Reservation confirmed = buildReservation("CONFIRMED");
        Reservation cancelled = buildReservation("CANCELLED");
        when(reservationMapper.selectById(RESERVATION_ID)).thenReturn(confirmed, cancelled);
        when(reservationMapper.transit(eq(RESERVATION_ID), eq("CONFIRMED"), eq("CANCELLED"), any(LocalDateTime.class)))
                .thenReturn(1);
        stubToVo();

        ReservationVO result = reservationService.cancelReservation(RESERVATION_ID, USER_ID, false);

        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        verify(resourceSlotMapper).release(SLOT_ID);
    }

    @Test
    @DisplayName("取消预约：COMPLETED 是终态，不允许再取消")
    void cancelCompletedIsRejected() {
        when(reservationMapper.selectById(RESERVATION_ID)).thenReturn(buildReservation("COMPLETED"));

        assertThatThrownBy(() -> reservationService.cancelReservation(RESERVATION_ID, USER_ID, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许从 COMPLETED 流转到 CANCELLED");

        verify(reservationMapper, never()).transit(any(), any(), any(), any());
        verify(resourceSlotMapper, never()).release(anyLong());
    }

    @Test
    @DisplayName("取消预约：不能操作他人的预约")
    void cancelOthersReservationIsRejected() {
        when(reservationMapper.selectById(RESERVATION_ID)).thenReturn(buildReservation("CONFIRMED"));

        assertThatThrownBy(() -> reservationService.cancelReservation(RESERVATION_ID, OTHER_USER_ID, false))
                .isInstanceOf(BusinessException.class)
                .hasMessage("无权操作他人的预约");

        verify(reservationMapper, never()).transit(any(), any(), any(), any());
    }

    // ── 状态流转 ─────────────────────────────────────────────────

    @Test
    @DisplayName("状态流转：CANCELLED 是终态，不能再流转")
    void changeStatusWithIllegalTransition() {
        when(reservationMapper.selectById(RESERVATION_ID)).thenReturn(buildReservation("CANCELLED"));

        assertThatThrownBy(() -> reservationService.changeStatus(RESERVATION_ID, "CONFIRMED", USER_ID, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许从 CANCELLED 流转到 CONFIRMED");

        verify(reservationMapper, never()).transit(any(), any(), any(), any());
    }

    @Test
    @DisplayName("状态流转：条件更新影响 0 行时提示刷新重试")
    void changeStatusWhenConcurrentlyModified() {
        when(reservationMapper.selectById(RESERVATION_ID)).thenReturn(buildReservation("CONFIRMED"));
        when(reservationMapper.transit(eq(RESERVATION_ID), eq("CONFIRMED"), eq("CANCELLED"), any(LocalDateTime.class)))
                .thenReturn(0);

        assertThatThrownBy(() -> reservationService.cancelReservation(RESERVATION_ID, USER_ID, true))
                .isInstanceOf(BusinessException.class)
                .hasMessage("预约状态已被并发修改，请刷新后重试");
    }

    // ── 签到 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("签到：时段开始 5 分钟内可签到")
    void checkInSuccess() {
        LocalDateTime start = LocalDateTime.now().minusMinutes(5);
        when(reservationMapper.selectById(RESERVATION_ID)).thenReturn(buildReservation("CONFIRMED"));
        when(resourceSlotMapper.selectById(SLOT_ID)).thenReturn(buildSlot(start));
        when(reservationMapper.checkInReservation(RESERVATION_ID)).thenReturn(1);

        assertThat(reservationService.checkInReservation(RESERVATION_ID, USER_ID, false)).isTrue();
    }

    @Test
    @DisplayName("签到：时段尚未开始，拒绝")
    void checkInBeforeStart() {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        when(reservationMapper.selectById(RESERVATION_ID)).thenReturn(buildReservation("CONFIRMED"));
        when(resourceSlotMapper.selectById(SLOT_ID)).thenReturn(buildSlot(start));

        assertThatThrownBy(() -> reservationService.checkInReservation(RESERVATION_ID, USER_ID, false))
                .isInstanceOf(BusinessException.class)
                .hasMessage("预约尚未开始，无法签到");

        verify(reservationMapper, never()).checkInReservation(anyLong());
    }

    @Test
    @DisplayName("签到：超过 15 分钟宽限期，拒绝")
    void checkInAfterGracePeriod() {
        LocalDateTime start = LocalDateTime.now().minusMinutes(30);
        when(reservationMapper.selectById(RESERVATION_ID)).thenReturn(buildReservation("CONFIRMED"));
        when(resourceSlotMapper.selectById(SLOT_ID)).thenReturn(buildSlot(start));

        assertThatThrownBy(() -> reservationService.checkInReservation(RESERVATION_ID, USER_ID, false))
                .isInstanceOf(BusinessException.class)
                .hasMessage("签到已过宽限期，无法签到");
    }

    @Test
    @DisplayName("签到：只有 CONFIRMED 状态可以签到")
    void checkInRequiresConfirmedStatus() {
        when(reservationMapper.selectById(RESERVATION_ID)).thenReturn(buildReservation("CANCELLED"));

        assertThatThrownBy(() -> reservationService.checkInReservation(RESERVATION_ID, USER_ID, false))
                .isInstanceOf(BusinessException.class)
                .hasMessage("预约状态不允许签到");

        verify(resourceSlotMapper, never()).selectById(anyLong());
    }

    // ── 定时任务 ─────────────────────────────────────────────────

    @Test
    @DisplayName("定时任务：已结束的 CONFIRMED 置为 COMPLETED，且不回补名额")
    void completeFinishedReservations() {
        Reservation finished = buildReservation("CONFIRMED");
        when(reservationMapper.selectFinishedConfirmed(any(LocalDateTime.class), eq(200)))
                .thenReturn(List.of(finished));
        when(reservationMapper.transit(eq(RESERVATION_ID), eq("CONFIRMED"), eq("COMPLETED"), any(LocalDateTime.class)))
                .thenReturn(1);

        assertThat(reservationService.completeFinishedReservations()).isEqualTo(1);

        // COMPLETED 不回补名额：时段已经结束
        verify(resourceSlotMapper, never()).release(anyLong());
    }

    @Test
    @DisplayName("定时任务：未签到的预约置为 EXPIRED 并回补名额")
    void expireUnCheckedInReservations() {
        Reservation expired = buildReservation("CONFIRMED");
        when(reservationMapper.selectExpiredCheckIn(any(LocalDateTime.class), eq(200), eq(15L)))
                .thenReturn(List.of(expired));
        when(reservationMapper.transit(eq(RESERVATION_ID), eq("CONFIRMED"), eq("EXPIRED"), any(LocalDateTime.class)))
                .thenReturn(1);

        assertThat(reservationService.expireUnCheckedInReservations()).isEqualTo(1);

        verify(resourceSlotMapper).release(SLOT_ID);
    }

    @Test
    @DisplayName("定时任务：状态已被并发修改时不计数")
    void expireUnCheckedInReservationsSkipsConcurrentModified() {
        Reservation expired = buildReservation("CONFIRMED");
        when(reservationMapper.selectExpiredCheckIn(any(LocalDateTime.class), eq(200), eq(15L)))
                .thenReturn(List.of(expired));
        when(reservationMapper.transit(eq(RESERVATION_ID), eq("CONFIRMED"), eq("EXPIRED"), any(LocalDateTime.class)))
                .thenReturn(0);

        assertThat(reservationService.expireUnCheckedInReservations()).isZero();
        verify(resourceSlotMapper, never()).release(anyLong());
    }

    // ── 查询权限 ─────────────────────────────────────────────────

    @Test
    @DisplayName("查询详情：本人可以查看")
    void getReservationByIdByOwner() {
        when(reservationMapper.selectById(RESERVATION_ID)).thenReturn(buildReservation("CONFIRMED"));
        stubToVo();

        ReservationVO result = reservationService.getReservationById(RESERVATION_ID, USER_ID, false);

        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getResourceName()).isEqualTo("A101 研修室");
    }

    @Test
    @DisplayName("查询详情：普通用户不能查看他人的预约")
    void getReservationByIdByOtherUserIsRejected() {
        when(reservationMapper.selectById(RESERVATION_ID)).thenReturn(buildReservation("CONFIRMED"));

        assertThatThrownBy(() -> reservationService.getReservationById(RESERVATION_ID, OTHER_USER_ID, false))
                .isInstanceOf(BusinessException.class)
                .hasMessage("无权查看他人的预约");
    }

    // ── 测试数据 ─────────────────────────────────────────────────

    private ResourceSlot buildSlot() {
        return buildSlot(LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0));
    }

    private ResourceSlot buildSlot(LocalDateTime start) {
        ResourceSlot slot = new ResourceSlot();
        slot.setSlotId(SLOT_ID);
        slot.setResourceId(RESOURCE_ID);
        slot.setSlotDate(start.toLocalDate());
        slot.setStartTime(start.toLocalTime());
        slot.setEndTime(start.plusHours(1).toLocalTime());
        slot.setBookingCapacity(1);
        slot.setReservedCount(0);
        slot.setStatus("AVAILABLE");
        return slot;
    }

    private Resource buildResource() {
        Resource resource = new Resource();
        resource.setResourceId(RESOURCE_ID);
        resource.setName("A101 研修室");
        resource.setStatus("ENABLED");
        return resource;
    }

    private ReservationDTO buildDTO() {
        return ReservationDTO.builder()
                .resourceId(RESOURCE_ID)
                .slotId(SLOT_ID)
                .requestId("req-0001")
                .remark("项目讨论")
                .build();
    }

    private Reservation buildReservation(String status) {
        Reservation reservation = new Reservation();
        reservation.setReservationId(RESERVATION_ID);
        reservation.setUserId(USER_ID);
        reservation.setSlotId(SLOT_ID);
        reservation.setRequestId("req-0001");
        reservation.setActiveKey(USER_ID + ":" + SLOT_ID);
        reservation.setStatus(status);
        return reservation;
    }

    /** toVO 需要回查时段与研修室，凡是会走到 toVO 的用例都要先打这两笔桩。 */
    private void stubToVo() {
        when(resourceSlotMapper.selectById(SLOT_ID)).thenReturn(buildSlot());
        when(resourceMapper.selectById(RESOURCE_ID)).thenReturn(buildResource());
    }
}
