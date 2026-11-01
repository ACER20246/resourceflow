-- resources/lua/slot_occupy.lua
--
-- 接口约定：
--   KEYS[1] = slot key
--   ARGV[1] = capacity（字符串，脚本里 tonumber）
--   ARGV[2] = owner（建议 userId:requestId）
--   ARGV[3] = ttl 秒
--
-- 返回：
--   0 成功 / 1 已满 / 2 已持有 / 3 参数错

local key      = KEYS[1]
local capacity = tonumber(ARGV[1])
local owner    = ARGV[2]
local ttl      = tonumber(ARGV[3])

-- ── 参数校验（放最前，非法输入不触碰任何 Redis 状态）──────────────
if key == nil or key == '' then
    return 3
end
-- capacity 必须是 >= 0 的整数；0 表示该时段不可约，比较时恒返回"已满"
if capacity == nil or capacity < 0 or capacity ~= math.floor(capacity) then
    return 3
end
if owner == nil or owner == '' then
    return 3
end
if ttl == nil or ttl <= 0 then
    return 3
end

local ownerField = 'o:' .. owner

-- ── ① 同一请求重复占位：命中即返回 2，绝不能走到 ③ 把 n 再加一次 ──
if redis.call('HEXISTS', key, ownerField) == 1 then
    return 2
end

-- ── ② 容量判断：HGET 字段不存在时返回 false，用 or '0' 兜底 ───────
local current = tonumber(redis.call('HGET', key, 'n') or '0')
if current >= capacity then
    return 1
end

-- ── ③ 三个写操作在同一个脚本里连续完成（原子）────────────────────
redis.call('HINCRBY', key, 'n', 1)
redis.call('HSET', key, ownerField, 1)
redis.call('EXPIRE', key, ttl)   -- 必须最后：只有写成功才落 TTL

return 0