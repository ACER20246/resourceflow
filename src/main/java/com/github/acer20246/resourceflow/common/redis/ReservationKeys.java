package com.github.acer20246.resourceflow.common.redis;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import com.github.acer20246.resourceflow.entity.ResourceSlot;

import static java.time.format.DateTimeFormatter.ofPattern;

public final class ReservationKeys {

    private static final int MAX_REQUEST_ID_LEN = 64;
    private static final java.util.regex.Pattern REQUEST_ID_PATTERN = java.util.regex.Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Generates a Redis key for a resource slot.
     * @param slot The ResourceSlot object containing the slot details.
     * @return Redis key in the format "slot:{resourceId}:{slotDate}:{startTime}:{endTime}".
     */
    public static String slot(ResourceSlot slot) {
        Objects.requireNonNull(slot, "slot must not be null");
        Objects.requireNonNull(slot.getSlotDate(), "slotDate must not be null");
        Objects.requireNonNull(slot.getStartTime(), "slotStartTime must not be null");
        Objects.requireNonNull(slot.getEndTime(), "slotEndTime must not be null");
        Objects.requireNonNull(slot.getResourceId(), "slotResourceId must not be null");
        return "slot:" + slot.getResourceId() + ":" + slot.getSlotDate().format(DATE)
                + ":" + slot.getStartTime().format(TIME) + ":"
                + slot.getEndTime().format(TIME);
    }

    /** 幂等 key：reservation:idempotent:{userId}:{requestId} */
    public static String idempotent(Long userId, String requestId) {
        Objects.requireNonNull(userId, "userId must not be null");
        validateRequestId(requestId);
        return "reservation:idempotent:" + userId + ":" + requestId;
    }

    private static void validateRequestId(String requestId) {
        if (requestId == null || requestId.isEmpty()) {
            throw new IllegalArgumentException("requestId must not be empty");
        }
        if (requestId.length() > MAX_REQUEST_ID_LEN) {
            throw new IllegalArgumentException("requestId too long, max " + MAX_REQUEST_ID_LEN);
        }
        if (!REQUEST_ID_PATTERN.matcher(requestId).matches()) {
            throw new IllegalArgumentException("requestId contains illegal characters: " + requestId);
        }
    }
}
