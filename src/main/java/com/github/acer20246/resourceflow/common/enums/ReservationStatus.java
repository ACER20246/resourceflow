package com.github.acer20246.resourceflow.common.enums;

import com.github.acer20246.resourceflow.common.exception.BusinessException;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 预约状态机。
 *
 * <pre>
 * CONFIRMED -> CANCELLED / COMPLETED / EXPIRED
 * CANCELLED / COMPLETED / EXPIRED 为终态，不可再流转
 * </pre>
 */
public enum ReservationStatus {

    /** 已确认：正常生效的预约，占用名额。 */
    CONFIRMED,

    /** 已取消：用户或管理员主动取消，名额已回补。 */
    CANCELLED,

    /** 已完成：时段正常结束，名额不再回补。 */
    COMPLETED,

    /** 已失效：时段开始后超过签到时限仍未签到，名额已回补。 */
    EXPIRED;

    private static final Map<ReservationStatus, Set<ReservationStatus>> TRANSITIONS = Map.of(
            CONFIRMED, Set.of(CANCELLED, COMPLETED, EXPIRED),
            CANCELLED, Set.of(),
            COMPLETED, Set.of(),
            EXPIRED, Set.of()
    );

    /** 占用名额、参与时间重叠校验的状态。 */
    private static final Set<String> OCCUPYING_STATUS_NAMES = Set.of(CONFIRMED.name());

    public boolean canTransitTo(ReservationStatus target) {
        if (target == null) {
            return false;
        }
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public boolean isOccupying() {
        return this == CONFIRMED;
    }

    public boolean isTerminal() {
        return TRANSITIONS.getOrDefault(this, Set.of()).isEmpty();
    }

    /**
     * 流转到该状态时是否需要回补 resource_slot.reserved_count。
     * CANCELLED / EXPIRED 回补；COMPLETED 不回补，因为时段已结束。
     */
    public boolean releasesCapacity() {
        return this == CANCELLED || this == EXPIRED;
    }

    /** 占用中的状态名集合，供 SQL 的 IN 条件使用。 */
    public static Set<String> occupyingNames() {
        return OCCUPYING_STATUS_NAMES;
    }

    public static ReservationStatus of(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("预约状态不能为空");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("非法的预约状态: " + value);
        }
    }
}