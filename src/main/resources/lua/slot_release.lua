-- resources/lua/slot_release.lua
--
-- 接口约定：
--   KEYS[1] = slot key
--   ARGV[1] = owner
--
-- 返回：
--   1 释放成功 / 0 不是你的占位

local key   = KEYS[1]
local owner = ARGV[1]

if key == nil or key == '' then
    return 0
end

if owner == nil or owner == '' then
    return 0
end

local ownerField = 'o:' .. owner

if redis.call('HEXISTS', key, ownerField) == 0 then
    return 0
end
-- 释放占位
redis.call('HDEL', key, ownerField)

local n=redis.call('HINCRBY', key, 'n', -1)
if n <0 then
    redis.call('HSET', key, 'n', 0)
end

if n == 0 then
    redis.call('DEL', key)
end

return 1