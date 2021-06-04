package com.example.redis.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ice
 * @blog https://blog.csdn.net/dreaming_coder
 * @description
 * @create 2021-05-29 20:01:38
 */
@Service
public class PathService {

    private RedisTemplate<String, Object> redisTemplate;

    private DefaultRedisScript<Long> script;

    @Autowired
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Autowired
    public void setScript(DefaultRedisScript<Long> script) {
        this.script = script;
    }

    public Long getDynamicPath(String goodId) {
        String key = goodId + ":path";
        List<String> keyList = new ArrayList<>();
        keyList.add(key);
        return redisTemplate.execute(script, keyList, randomSeed());
    }

    private static long randomSeed() {
        long temp = System.currentTimeMillis();
        long seed = 0L;
        while (temp > 0) {
            seed *= 10;
            seed += temp % 10;
            temp /= 10;
        }
        return seed % 100000000;
    }
}
