-- 锁的key
local key = KEYS[1];
-- 线程唯一标识
local threadId = ARGV[1];
-- 锁的自动释放时间
local releaseTime = ARGV[2];
-- 判断当前锁是否还是被自己持有
if (redis.call('HEXISTS', key, threadId) == 0) then
    -- 如果已经不是自己，则直接放回
    return nil;
end ;
-- 是自己的锁，则重入次数-1
local count = redis.call('HINCBY', key, threadId, -1);
-- 判断是否冲入次数是否已经为0
if (count > 0) then
    -- 大于0说明不能释放锁，重置有效期然后放回
    redis.call('EXPIRE', key, releaseTime);
    return nil;
else
    -- 等于0说明可以释放锁，直接删除
    redis.call('DEL', key);
    return nil;
end ;