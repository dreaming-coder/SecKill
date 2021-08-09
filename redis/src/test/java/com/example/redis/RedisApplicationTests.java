//package com.example.redis;
//
//import com.example.redis.service.LimitService;
//import com.example.redis.service.PathService;
//import com.example.redis.service.StockService;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.core.script.DefaultRedisScript;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Set;
//
//@SpringBootTest
//class RedisApplicationTests {
//
//    @Autowired
//    private RedisTemplate<String, Object> redisTemplate;
//
//    @Autowired
//    private StockService stockService;
//
//    @Autowired
//    private PathService pathService;
//
//    @Autowired
//    private LimitService limitService;
//
//    @Autowired
//    private DefaultRedisScript<Boolean> checkStockScript;
//
//    @Autowired
//    private DefaultRedisScript<Long> pathScript;
//
//    @Test
//    @DisplayName("测试读取 Lua 脚本")
//    void test0(){
//        System.out.println(checkStockScript.getScriptAsString());
//        System.out.println(pathScript.getScriptAsString());
//    }
//
//    @Test
//    @DisplayName("测试Redis连接")
//    void test1() {
//        redisTemplate.opsForValue().set("k1", "v1");
//        Object k1 = redisTemplate.opsForValue().get("k1");
//        System.out.println(k1);
//        redisTemplate.delete("k1");
//        k1 = redisTemplate.opsForValue().get("k1");
//        System.out.println(k1);
//        Boolean a = redisTemplate.opsForValue().setIfAbsent("kk", "vv");
//        System.out.println(a);
//    }
//
//    @Test
//    @DisplayName("测试 StockService")
//    void test2(){
//        Map<String,Object> map = new HashMap<>();
//        map.put("2020",1);
//        map.put("2021",60);
//        map.put("2022",99);
//        stockService.initStock(map);
//        stockService.decrementStock("2020");
//        stockService.incrementStock("2022");
//        Set<String> keys = redisTemplate.keys("*");
//        assert keys != null;
//        keys.forEach(
//                k -> System.out.println(k + ": " + redisTemplate.opsForValue().get(k))
//        );
//        System.out.println("===========================================");
//
//        Boolean a = stockService.checkStock("2020");
//        Boolean b = stockService.checkStock("2021");
//        System.out.println(a);
//        System.out.println(b);
//        System.out.println("===========================================");
//
//        keys = redisTemplate.keys("*");
//        assert keys != null;
//        keys.forEach(
//                k -> System.out.println(k + ": " + redisTemplate.opsForValue().get(k))
//        );
//
//        redisTemplate.delete(keys);
//    }
//
//    @Test
//    @DisplayName("测试 PathService")
//    void test3(){
//        Long dynamicPath = pathService.getDynamicPath("2020");
//        System.out.println(dynamicPath);
//    }
//
//    @Test
//    @DisplayName("测试 LimitService")
//    void test4(){
//        Boolean b = limitService.checkRepeatLimit("2022", "17512511216","644476114@qq.com");
//        System.out.println(b);
//        Object o = redisTemplate.opsForValue().get("2022:17512511216");
//        System.out.println(o);
//    }
//}
