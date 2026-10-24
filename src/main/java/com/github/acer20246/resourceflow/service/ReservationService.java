package com.github.acer20246.resourceflow.service;

import com.github.acer20246.resourceflow.common.result.PageResult;
import com.github.acer20246.resourceflow.dto.ReservationDTO;
import com.github.acer20246.resourceflow.dto.ReservationPageQueryDTO;
import com.github.acer20246.resourceflow.vo.ReservationVO;

public interface ReservationService {

    /**
     * 查询预约详情。普通用户只能查看自己的预约。
     *
     * @param reservationId 预约 ID
     * @param operatorId    操作人 ID
     * @param admin         是否为管理员
     */
    ReservationVO getReservationById(Long reservationId, Long operatorId, boolean admin);

    /**
     * 创建预约：幂等校验 -> 锁用户 -> 时段校验 -> 时间重叠校验 -> 原子扣减名额 -> 落库。
     *
     * @param userId 预约用户 ID
     * @param dto    创建预约请求
     */
    ReservationVO createReservation(Long userId, ReservationDTO dto);

    /**
     * 分页查询当前用户的预约。
     */
    PageResult<ReservationVO> getMyReservations(Long userId, ReservationPageQueryDTO queryDTO);

    /**
     * 取消预约。普通用户只能取消自己的预约。
     *
     * @param reservationId 预约 ID
     * @param operatorId    操作人 ID
     * @param admin         是否为管理员
     */
    ReservationVO cancelReservation(Long reservationId, Long operatorId, boolean admin);

    /**
     * 按状态机修改预约状态，供用户端与管理员端复用。
     *
     * @param reservationId 预约 ID
     * @param targetStatus  目标状态
     * @param operatorId    操作人 ID
     * @param admin         是否为管理员
     */
    ReservationVO changeStatus(Long reservationId, String targetStatus, Long operatorId, boolean admin);

    /**
     * 将已结束的 CONFIRMED 预约批量置为 COMPLETED。
     *
     * @return 处理条数
     */
    int completeFinishedReservations();
}
