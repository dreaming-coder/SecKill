package com.example.redis.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author ice
 * @blog https://blog.csdn.net/dreaming_coder
 * @description
 * @create 2021-05-29 18:39:57
 */
@Service
public class StockService {

    private RedisTemplate<String, Object> redisTemplate;

    private DefaultRedisScript<Boolean> script;

    @Autowired
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Autowired
    public void setScript(DefaultRedisScript<Boolean> script) {
        this.script = script;
    }

    public void initStock(Map<String, Object> map) {
        map.forEach(
                (goodId, stock) -> {
                    String key = goodId + ":stock";
                    redisTemplate.opsForValue().set(key, stock);
                }
        );
    }

    public Long getStock(String goodId) {
        if (null == goodId) {
            return -1L;
        }
        String key = goodId + ":stock";
        Object stock = redisTemplate.opsForValue().get(key);
        return null == stock ? -1L : Long.parseLong(stock.toString());
    }

    public void decrementStock(String goodId) {
        String key = goodId + ":stock";
        redisTemplate.opsForValue().decrement(key, 1);
    }

    public void resetStock(String goodId) {
        String key = goodId + ":stock";
        redisTemplate.opsForValue().set(key, 0);
    }

    public void incrementStock(String goodId) {
        String key = goodId + ":stock";
        redisTemplate.opsForValue().increment(key, 1);
    }

    public Boolean checkStock(String goodId) {
        String key = goodId + ":stock";
        List<String> keyList = new ArrayList<>();
        keyList.add(key);
        return redisTemplate.execute(script, keyList);
    }
}
