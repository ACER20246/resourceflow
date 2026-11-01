package com.github.acer20246.resourceflow;

import com.github.acer20246.resourceflow.service.ReservationGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ActiveProfiles({"dev","test"})
@ExtendWith(MockitoExtension.class)
class ReservationGuardTest {

    @Mock
    StringRedisTemplate redis;
    @Mock
    DefaultRedisScript<Long> occupyScript;
    @Mock
    DefaultRedisScript<Long> releaseScript;
    ReservationGuard guard;

    @BeforeEach
    void setUp() {
        guard = new ReservationGuard(redis, occupyScript, releaseScript);
        ReflectionTestUtils.setField(guard, "enabled", true);
        ReflectionTestUtils.setField(guard, "slotTtlSeconds", 300);
    }

    @Test
    void occupy_degrades_when_redis_down() {
        when(redis.execute(any(), anyList(), any(), any(), any()))
                .thenThrow(new RedisConnectionFailureException("down"));
        assertEquals(ReservationGuard.OccupyResult.DEGRADED, guard.tryOccupy("k", 1, "t"));
    }

    @Test
    void release_never_throws() {
        doThrow(new RedisConnectionFailureException("down")).when(redis).execute(any(), anyList(), any());
        assertDoesNotThrow(() -> guard.release("k", "t"));
    }
}
