local function getRandom(n,seed)
    math.randomseed(seed)
    local t = {
        "0","1","2","3","4","5","6","7","8","9"
    }
    local s = ""
    for i =1, n do
        s = s .. t[math.random(#t)]
    end;
    return s
end;

local path = redis.call('GET', KEYS[1])

if not path then
    path = tonumber(getRandom(8, ARGV[1]))
    redis.call('SETEX',KEYS[1],60, path)
    return path
else
    return tonumber(path)
end