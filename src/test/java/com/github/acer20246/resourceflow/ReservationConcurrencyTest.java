package com.github.acer20246.resourceflow;

import com.github.acer20246.resourceflow.common.exception.BusinessException;
import com.github.acer20246.resourceflow.dto.ReservationDTO;
import com.github.acer20246.resourceflow.service.ReservationGuard;
import com.github.acer20246.resourceflow.service.ReservationService;
import com.github.acer20246.resourceflow.vo.ReservationVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Day 12 验收：同一资源同一时段，并发下只能成功一次。
 *
 * <p>刻意不使用 @Transactional：多线程各自开连接，测试方法上的事务对它们不可见，
 * 断言会虚假通过。全程直连真实 MySQL（resourceflow_test）与真实 Redis（db 1）。</p>
 */
@SpringBootTest
@ActiveProfiles({"dev", "test"})
class ReservationConcurrencyTest {

    private static final int THREADS = 30;
    private static final long RESOURCE_ID = 9001L;
    private static final long SLOT_ID = 9001L;
    private static final long USER_ID_BASE = 9100L;
    private static final LocalDate SLOT_DATE = LocalDate.now().plusDays(1);

    @Autowired private ReservationService reservationService;
    @Autowired private ReservationGuard guard;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private StringRedisTemplate redis;

    @BeforeEach
    void setUp() {
        // 顺序无所谓（1.sql 里没有外键），但从子到父删更安全
        jdbc.update("DELETE FROM reservation");
        jdbc.update("DELETE FROM resource_slot");
        jdbc.update("DELETE FROM resource");
        jdbc.update("DELETE FROM `user`");

        jdbc.update("INSERT INTO resource(resource_id, category_id, name, location, capacity, status) "
                + "VALUES (?, 1, ?, 'A-101', 10, 'ENABLED')", RESOURCE_ID, "并发测试室");

        jdbc.update("INSERT INTO resource_slot(slot_id, resource_id, slot_date, start_time, end_time, "
                        + "booking_capacity, reserved_count, version, status) "
                        + "VALUES (?, ?, ?, '09:00:00', '10:00:00', 1, 0, 0, 'AVAILABLE')",
                SLOT_ID, RESOURCE_ID, SLOT_DATE);

        for (int i = 0; i < THREADS; i++) {
            jdbc.update("INSERT INTO `user`(user_id, username, password, role, status) "
                    + "VALUES (?, ?, 'x', 'USER', 'ENABLED')", USER_ID_BASE + i, "concurrent_u" + i);
        }

        // 严格模式是「用超长 remark 制造 insert 失败」这个用例的前提
        String sqlMode = jdbc.queryForObject("SELECT @@SESSION.sql_mode", String.class);
        assertTrue(sqlMode != null && sqlMode.toUpperCase().contains("STRICT"),
                "MySQL 未开启严格模式，超长 remark 会被静默截断，补偿用例将失去意义: " + sqlMode);

        clearTestRedis();
    }

    @AfterEach
    void tearDown() {
        ReflectionTestUtils.setField(guard, "enabled", true);
    }

    // ── 用例 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("闸门开启：30 人抢 1 个名额，恰好成功 1 个")
    void onlyOneSucceedsWithGateOn() throws Exception {
        int success = runConcurrently(THREADS, i -> new Attempt(USER_ID_BASE + i, dto("req-" + i)));

        assertExactlyOne(success);
        assertEquals("1", redis.opsForHash().get(slotKey(), "n"),
                "30 个并发过后 Redis 计数必须正好是 1，多了说明 Lua 脚本没兜住");
        assertTrue(Boolean.TRUE.equals(redis.hasKey(slotKey())), "成功路径不释放占位，靠 TTL 自然过期");
    }

    @Test
    @DisplayName("闸门关闭：压力全打到数据库，仍然恰好成功 1 个")
    void onlyOneSucceedsWithGateOff() throws Exception {
        ReflectionTestUtils.setField(guard, "enabled", false);

        int success = runConcurrently(THREADS, i -> new Attempt(USER_ID_BASE + i, dto("req-" + i)));

        assertExactlyOne(success);
        assertFalse(Boolean.TRUE.equals(redis.hasKey(slotKey())), "降级时不应往 Redis 写任何东西");
    }

    @Test
    @DisplayName("同一 requestId 并发 10 次：只落 1 行，且返回同一个 reservationId")
    void sameRequestIdProducesSingleReservation() throws Exception {
        List<Attempt> attempts = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            attempts.add(new Attempt(USER_ID_BASE, dto("same-req")));
        }

        List<ReservationVO> succeeded = runAndCollect(attempts);

        assertEquals(1, countActiveReservations(), "同一 requestId 只能产生 1 行");
        assertFalse(succeeded.isEmpty(), "至少应有一个线程拿到结果");
        Long first = succeeded.get(0).getReservationId();
        assertNotNull(first);
        for (ReservationVO vo : succeeded) {
            assertEquals(first, vo.getReservationId(), "幂等命中必须返回同一个预约");
        }
    }

    @Test
    @DisplayName("落库失败：Redis 占位被补偿释放，同一 requestId 可以重试")
    void releaseSlotWhenInsertFails() {
        long userId = USER_ID_BASE;

        ReservationDTO bad = dto("fail-req");
        bad.setRemark("x".repeat(600));   // remark 是 varchar(500)，insert 必然失败

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reservationService.createReservation(userId, bad));
        assertFalse(ex instanceof BusinessException,
                "期望数据库写入失败，实际是业务校验先挂了: " + ex.getMessage());

        // 事务回滚：DB 名额还回去了
        assertEquals(0, countReserved(), "事务回滚后 reserved_count 必须归零");
        // 补偿释放：Redis 占位被删掉，而不是等 TTL
        assertFalse(Boolean.TRUE.equals(redis.hasKey(slotKey())),
                "占位没有被释放，n 仍存在说明 catch 里调错了方法");
        assertFalse(Boolean.TRUE.equals(redis.hasKey(idemKey(userId, "fail-req"))),
                "幂等键没清，用户会被自己上一次的失败挡住");

        // 同一个 requestId 现在应该能正常约上
        ReservationVO vo = reservationService.createReservation(userId, dto("fail-req"));
        assertNotNull(vo.getReservationId());
        assertEquals(1, countActiveReservations());
    }

    @RepeatedTest(5)
    @DisplayName("闸门开启连跑 5 轮：每轮都只成功 1 个（换成 50 轮就是压力测试）")
    void stableAcrossRounds() throws Exception {
        int success = runConcurrently(THREADS, i -> new Attempt(USER_ID_BASE + i, dto("round-" + i)));
        assertExactlyOne(success);
    }

    // ── 断言 ─────────────────────────────────────────────────────

    private void assertExactlyOne(int success) {
        assertEquals(1, success, "只允许一个请求成功");
        assertEquals(1, countReserved(), "reserved_count 必须等于成功数");
        assertEquals(1, countActiveReservations(), "活跃预约行数必须等于成功数");
    }

    private int countReserved() {
        return jdbc.queryForObject(
                "SELECT reserved_count FROM resource_slot WHERE slot_id = ?", Integer.class, SLOT_ID);
    }

    private int countActiveReservations() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM reservation WHERE slot_id = ? AND status = 'CONFIRMED'",
                Integer.class, SLOT_ID);
    }

    // ── 并发工具 ─────────────────────────────────────────────────

    private record Attempt(long userId, ReservationDTO dto) {
    }

    /** 就位即等，start 一次性放行；业务失败计 0，非业务异常直接抛出来。 */
    private int runConcurrently(int n, IntFunction<Attempt> factory) throws Exception {
        List<Future<Boolean>> futures = new ArrayList<>(n);
        AtomicReference<Throwable> unexpected = new AtomicReference<>();
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(n);

        try {
            for (int i = 0; i < n; i++) {
                Attempt attempt = factory.apply(i);
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        reservationService.createReservation(attempt.userId(), attempt.dto());
                        return true;
                    } catch (BusinessException e) {
                        return false;
                    } catch (Throwable t) {
                        unexpected.compareAndSet(null, t);
                        return false;
                    }
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS), "线程未能全部就位");
            start.countDown();

            int success = 0;
            for (Future<Boolean> f : futures) {
                if (Boolean.TRUE.equals(f.get(60, TimeUnit.SECONDS))) {
                    success++;
                }
            }
            if (unexpected.get() != null) {
                throw new AssertionError("出现非业务异常", unexpected.get());
            }
            return success;
        } finally {
            pool.shutdownNow();
        }
    }

    /** 收集成功返回的 VO；BusinessException（已满 / 处理中）视为合法失败。 */
    private List<ReservationVO> runAndCollect(List<Attempt> attempts) throws Exception {
        int n = attempts.size();
        List<Future<ReservationVO>> futures = new ArrayList<>(n);
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(n);

        try {
            for (Attempt attempt : attempts) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        return reservationService.createReservation(attempt.userId(), attempt.dto());
                    } catch (BusinessException e) {
                        return null;
                    }
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS), "线程未能全部就位");
            start.countDown();

            List<ReservationVO> succeeded = new ArrayList<>();
            for (Future<ReservationVO> f : futures) {
                ReservationVO vo = f.get(60, TimeUnit.SECONDS);
                if (vo != null) {
                    succeeded.add(vo);
                }
            }
            return succeeded;
        } finally {
            pool.shutdownNow();
        }
    }

    // ── 环境 ─────────────────────────────────────────────────────

    private ReservationDTO dto(String requestId) {
        return ReservationDTO.builder()
                .resourceId(RESOURCE_ID)
                .slotId(SLOT_ID)
                .requestId(requestId)
                .build();
    }

    /** 与 ReservationKeys.slot() 的实际格式对齐：slot:{resourceId}:{date}:{HH:mm}:{HH:mm}。 */
    private String slotKey() {
        return "slot:" + RESOURCE_ID + ":" + SLOT_DATE + ":09:00:10:00";
    }

    private String idemKey(long userId, String requestId) {
        return "reservation:idempotent:" + userId + ":" + requestId;
    }

    /** 只删测试用的两类 key，绝不 flushDb —— 万一 profile 没生效，也不会清掉开发数据。 */
    private void clearTestRedis() {
        deleteByPattern("slot:*");
        deleteByPattern("reservation:idempotent:*");
    }

    private void deleteByPattern(String pattern) {
        Set<String> keys = redis.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }
}
