-- id参数
local voucherId = ARGV[1]
local userId = ARGV[2]
-- 数据key
local stockKey = "seckill:stock:" .. voucherId
local orderKey = "seckill:order:" .. voucherId
-- 判断库存是否充足
if (tonumber(redis.call('get', stockKey)) <= 0) then
    return 1 -- 库存不足
end
-- 判断用户一人一单（set集合，userId为value值）
if (redis.call('sismember', orderKey, userId) == 1) then
    return 2 -- 重复下单
end
-- 预扣库存，下单
redis.call('incrby', stockKey, -1)
redis.call('sadd', orderKey, userId)
return 0 -- 可以抢购