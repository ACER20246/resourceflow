package com.github.acer20246.resourceflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 预约链路的前置闸门。
 *
 * <p>职责只有一个：把「Redis 可能不可用」挡在业务代码之外。所有方法内部消化异常，
 * 以 DEGRADED 表示"闸门失效，请继续走数据库"。正确性始终由数据库约束保证，
 * 本类只影响性能与拦截率。</p>
 */
@Slf4j
@Component
public class ReservationGuard {

    /** 幂等键：处理中标记，后接 owner token。 */
    private static final String IDEM_PROCESSING = "PROCESSING:";
    /** 幂等键：已完成标记，后接 reservationId。 */
    private static final String IDEM_DONE = "DONE:";
    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> occupyScript;
    private final DefaultRedisScript<Long> releaseScript;
    @Value("${resourceflow.reservation.redis.enabled:true}")
    private boolean enabled;
    /**幂等键，覆盖当天重试窗口*/
    @Value("${resourceflow.reservation.redis.idempotent-ttl-seconds:86400}")
    private Long idempotentTtlSeconds;
    @Value("${resourceflow.reservation.redis.slot-ttl-seconds:60}")
    private int slotTtlSeconds;
    @Value("${resourceflow.reservation.redis.processing-ttl-seconds:60}")
    private long processingTtlSeconds;


    public ReservationGuard(StringRedisTemplate redis, @Qualifier("slotOccupyScript") DefaultRedisScript<Long> occupyScript, @Qualifier("slotReleaseScript") DefaultRedisScript<Long> releaseScript) {
        this.redis = redis;
        this.occupyScript = occupyScript;
        this.releaseScript = releaseScript;
    }

    /**
     * 预订占用结果
     */
    public enum OccupyResult {
        OK,     // 成功
        ALREADY_OCCUPIED, // 已被占用
        FULL,   // 已满
        DEGRADED    // Redis 异常，降级处理
    }
    /**
     * 幂等状态
     */
    public enum IdemState {
        ACQUIRED,   //成功
        IN_PROGRESS,    //处理中
        DONE,   //已经处理
        DEGRADED    //降级，异常
    }

    /**
     * 幂等结果
     */
    public record IdemOutcome(IdemState state, Long reservationId) {
        static IdemOutcome acquired()    { return new IdemOutcome(IdemState.ACQUIRED, null); }
        static IdemOutcome inProgress()  { return new IdemOutcome(IdemState.IN_PROGRESS, null); }
        static IdemOutcome degraded()    { return new IdemOutcome(IdemState.DEGRADED, null); }
        static IdemOutcome done(Long id) { return new IdemOutcome(IdemState.DONE, id); }
    }
    /** owner token 统一由 userId:requestId 构成，避免不同用户撞同一个占位标记。 */
    public static String token(Long userId,String requestId){
        return userId + ":" + requestId;
    }

    /**
     * 尝试获取幂等锁
     */
    public IdemOutcome tryAcquire(String key,String token){
        if(!enabled) {
            return IdemOutcome.degraded();
        }
        String holdValue = IDEM_PROCESSING + token;
        Duration ttl = Duration.ofSeconds(processingTtlSeconds);
        try{
            if(Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key,holdValue,ttl))){
                return IdemOutcome.acquired();
            }
            String existing = redis.opsForValue().get(key);
            if(existing==null){
                return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key,holdValue,ttl))
                        ? IdemOutcome.acquired()
                        : IdemOutcome.inProgress();
            }
            if(existing.startsWith(IDEM_DONE)){
                log.info("幂等键 {} 已标记为 DONE", key);
                Long reservationId = parseReservationId(existing);
                if(reservationId!=null){
                    return IdemOutcome.done(reservationId);
                }
            }
            return IdemOutcome.inProgress();
        }catch (RuntimeException e) {
            log.error("幂等键 {} 处理时发生异常：{}", key,e.toString());
            return IdemOutcome.degraded();
        }
    }

    /**
     * 尝试占用资源
     */
    public OccupyResult tryOccupy(String key,int capacity, String token) {
        if(!enabled) {
            return OccupyResult.DEGRADED;
        }
        if(capacity <= 0) {
            log.warn("容量参数 capacity 必须大于 0，当前值为 {}", capacity);
            return OccupyResult.FULL;
        }
        try {
            Long result = redis.execute(occupyScript, List.of(key), String.valueOf(capacity), token, String.valueOf(slotTtlSeconds));
            if (result == null) {
                log.error("Redis 脚本执行返回 null，可能是 Redis 异常");
                return OccupyResult.DEGRADED;
            }
            switch (result.intValue()) {
                case 0 -> {
                    return OccupyResult.OK;
                }                case 1 -> {
                    return OccupyResult.FULL;
                }
                case 2 -> {
                    return OccupyResult.ALREADY_OCCUPIED;
                }
                default -> {
                    log.error("Redis 脚本返回未知结果：{}", result);
                    return OccupyResult.DEGRADED;
                }
            }
        } catch (RuntimeException e) {
            log.error("Redis 脚本执行异常：{}", e.toString());
            return OccupyResult.DEGRADED;
        }
    }

    public void release(String key, String token) {
        if(!enabled) {
            return;
        }
        try {
            Long result = redis.execute(releaseScript, List.of(key), token);
            if (result == null|| result == 0L) {
                log.error("占位不存在或不是持有者，无法释放：key={}, token={}", key, token);
            }
        } catch (RuntimeException e) {
            log.error("释放占位时发生异常：key={}, token={}, error={}", key, token, e.toString());
        }
    }
    /**
     * 标记幂等键为已完成,幂等键的值将被设置为 DONE:reservationId，并设置过期时间为 idempotentTtlSeconds。
     * @param key 幂等键
     * @param reservationId 预约 ID
     */
    public void markDone(String key, Long reservationId) {
        if(!enabled) {
            return;
        }
        String doneValue = IDEM_DONE + reservationId;
        Duration ttl = Duration.ofSeconds(idempotentTtlSeconds);
        try {
            redis.opsForValue().set(key, doneValue, ttl);
            log.info("幂等键 {} 已标记为 DONE，reservationId={}", key, reservationId);
        } catch (RuntimeException e) {
            log.error("标记幂等键 {} 为 DONE 时发生异常：{}", key, e.toString());
        }
    }
    public void clear(String key, String token) {
        if(!enabled) {
            return;
        }
        try {
            if((IDEM_PROCESSING+token).equals(redis.opsForValue().get(key))) {
                redis.delete(key);
                log.info("幂等键 {} 已清理", key);
            }
        } catch (RuntimeException e) {
            log.error("清理幂等键 {} 时发生异常：{}", key, e.toString());
        }
    }
    private static Long parseReservationId(String value) {
        try {
            return Long.parseLong(value.substring(IDEM_DONE.length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
