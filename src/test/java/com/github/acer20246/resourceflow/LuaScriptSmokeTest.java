package com.github.acer20246.resourceflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class LuaScriptSmokeTest {

    @Autowired
    StringRedisTemplate redis;
    @Autowired @Qualifier("slotOccupyScript")
    DefaultRedisScript<Long> occupy;
    @Autowired @Qualifier("slotReleaseScript") DefaultRedisScript<Long> release;

    @Test
    void script_should_work() {
        String key = "test:lua:verify";
        redis.delete(key);
        List<String> keys = List.of(key);

        assertEquals(0L, redis.execute(occupy, keys, "1", "owner-a", "60"));
        assertEquals(1L, redis.execute(occupy, keys, "1", "owner-b", "60"));  // 已满
        assertEquals(2L, redis.execute(occupy, keys, "1", "owner-a", "60"));  // 已持有
        assertEquals(0L, redis.execute(release, keys, "owner-c"));            // 不是他的
        assertEquals(1L, redis.execute(release, keys, "owner-a"));            // 释放成功
        assertEquals(0L, redis.execute(occupy, keys, "1", "owner-b", "60"));  // 现在能占

        redis.delete(key);
    }
}
