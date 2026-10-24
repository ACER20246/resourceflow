package com.github.acer20246.resourceflow.controller;

import com.github.acer20246.resourceflow.common.utils.SecurityUtils;
import com.github.acer20246.resourceflow.dto.ReservationCancelDTO;
import com.github.acer20246.resourceflow.service.ReservationService;
import com.github.acer20246.resourceflow.vo.ReservationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员预约接口。
 *
 * <p>路径以 {@code /api/admin/**} 开头，由 SecurityConfig 限制为 ADMIN 角色。
 * 状态变更统一走状态机，非法流转返回 400。</p>
 */
@RestController
@RequestMapping("/api/admin/reservations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "管理员-预约接口", description = "管理员查询与流转预约状态")
public class AdminReservationController {

    private final ReservationService reservationService;

    @Operation(summary = "修改预约状态", description = "按状态机流转，非法流转返回 400")
    @PatchMapping("/{reservationId}/status")
    public ReservationVO changeStatus(@PathVariable Long reservationId,String status) {
        log.info("管理员修改预约状态: {}, target: {}", reservationId, status);
        return reservationService.changeStatus(
                reservationId,
                status,
                SecurityUtils.currentUserId(),
                true);
    }

    @Operation(summary = "管理员取消预约")
    @PostMapping("/{reservationId}/cancel")
    public ReservationVO cancel(@PathVariable Long reservationId, @RequestBody(required = false) ReservationCancelDTO cancelDTO) {
        log.info("管理员取消预约: {}", reservationId);
        return reservationService.cancelReservation(
                reservationId,
                SecurityUtils.currentUserId(),
                true);
    }
}
