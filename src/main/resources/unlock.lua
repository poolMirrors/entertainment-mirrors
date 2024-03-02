-- 锁的key
local key = KEYS[1]
-- 当前线程标识
local curThreadId = ARGV[1]
-- 查redis拿到 获取锁的线程 的唯一标识
local id = redis.call('get', key)
-- 比较，是自己的锁才释放
if (curThreadId == id) then
    -- 释放锁
    return redis.call('del', key)
end
return 0