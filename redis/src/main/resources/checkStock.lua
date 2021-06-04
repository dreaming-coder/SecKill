local stock = redis.call('GET', KEYS[1])

if tonumber(stock) > 0 then
    redis.call('DECR', KEYS[1])
    return true
else
    return false
end