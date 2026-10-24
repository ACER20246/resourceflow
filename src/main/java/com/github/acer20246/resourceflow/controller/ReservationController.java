package com.github.acer20246.resourceflow.controller;

import com.github.acer20246.resourceflow.common.enums.ReservationStatus;
import com.github.acer20246.resourceflow.common.result.PageResult;
import com.github.acer20246.resourceflow.common.utils.SecurityUtils;
import com.github.acer20246.resourceflow.dto.ReservationCancelDTO;
import com.github.acer20246.resourceflow.dto.ReservationDTO;
import com.github.acer20246.resourceflow.dto.ReservationPageQueryDTO;
import com.github.acer20246.resourceflow.service.ReservationService;
import com.github.acer20246.resourceflow.vo.ReservationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 预约接口：创建、查询、取消与完成。
 *
 * <p>用户 ID 与管理员身份均从 JWT 解析，普通用户只能操作自己的预约；
 * 创建接口依赖 {@code requestId} 做幂等，重复提交返回同一笔预约。</p>
 */
@RestController
@RequestMapping("/api/reservations")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "预约接口", description = "预约的创建、查询、取消与完成")
public class ReservationController {

    private final ReservationService reservationService;

    @Operation(summary = "查询预约详情")
    @GetMapping("/{reservationId}")
    public ReservationVO getReservationById(@PathVariable Long reservationId) {
        log.info("获取预约信息: {}", reservationId);
        return reservationService.getReservationById(
                reservationId, SecurityUtils.currentUserId(), SecurityUtils.isAdmin());
    }

    @Operation(summary = "创建预约", description = "同一 requestId 重复提交返回同一笔预约；时段冲突返回 400")
    @PostMapping
    public ReservationVO createReservation(@RequestBody ReservationDTO reservationDTO) {
        log.info("创建预约: {}", reservationDTO);
        return reservationService.createReservation(SecurityUtils.currentUserId(), reservationDTO);
    }

    @Operation(summary = "查询我的预约")
    @GetMapping("/my")
    public PageResult<ReservationVO> getMyReservations(ReservationPageQueryDTO queryDTO) {
        log.info("获取我的预约: {}", queryDTO);
        return reservationService.getMyReservations(SecurityUtils.currentUserId(), queryDTO);
    }

    @Operation(summary = "取消预约")
    @PostMapping("/{reservationId}")
    public ReservationVO cancelReservation(@PathVariable Long reservationId) {
        log.info("取消预约: {}", reservationId);
        return reservationService.cancelReservation(
                reservationId,
                SecurityUtils.currentUserId(),
                SecurityUtils.isAdmin());
    }

    @Operation(summary = "完成预约", description = "用户主动结束使用；标记完成后名额不再回补")
    @PostMapping("/{reservationId}/complete")
    public ReservationVO completeReservation(@PathVariable Long reservationId) {
        log.info("完成预约: {}", reservationId);
        return reservationService.changeStatus(
                reservationId,
                ReservationStatus.COMPLETED.name(),
                SecurityUtils.currentUserId(),
                SecurityUtils.isAdmin());
    }
}
