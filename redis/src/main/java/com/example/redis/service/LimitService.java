package com.example.redis.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * @author ice
 * @blog https://blog.csdn.net/dreaming_coder
 * @description
 * @create 2021-05-29 22:30:08
 */
@Service
public class LimitService {

    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Boolean checkRepeatLimit(String goodId, String phone, String email) {
        String key = goodId + ":" + phone;
        return redisTemplate.opsForValue().setIfAbsent(key, email, Duration.ofSeconds(10));
    }

}
