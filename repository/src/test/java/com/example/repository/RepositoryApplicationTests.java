//package com.example.repository;
//
//import com.example.repository.entity.Good;
//import com.example.repository.entity.Order;
//import com.example.repository.mapper.GoodMapper;
//import com.example.repository.mapper.OrderMapper;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import javax.sql.DataSource;
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.List;
//
//@SpringBootTest
//@Slf4j
//class RepositoryApplicationTests {
//    @Autowired
//    private DataSource dataSource;
//
//    @Autowired
//    private GoodMapper goodMapper;
//
//    @Autowired
//    private OrderMapper orderMapper;
//
//    @Test
//    @DisplayName("测试数据源")
//    void test1() {
//        System.out.println("数据源： " + dataSource.getClass());
//    }
//
//    @Test
//    @DisplayName("测试 sec_orders 表增删改查")
//    void test2() {
//        log.info("插入记录");
//        Order order = new Order();
//        order.setEmail("644476114@qq.com");
//        order.setGoodId("20191221024");
//        order.setPhone("17512511216");
//        order.setStatus("1");
//        int insert = orderMapper.insert(order);
//        log.info("查询记录");
//        List<Order> orders = orderMapper.selectList(null);
//        orders.forEach(System.out::println);
//        log.info("修改记录");
//        order = orders.get(0);
//        order.setStatus("0");
//        int update = orderMapper.updateById(order);
//        orders = orderMapper.selectList(null);
//        orders.forEach(System.out::println);
//        log.info("删除记录");
//        orderMapper.deleteById(order);
//        orders = orderMapper.selectList(null);
//        orders.forEach(System.out::println);
//    }
//
//    @Test
//    @DisplayName("测试 sec_goods 表增删改查")
//    void test3() {
//        log.info("插入记录");
//        Good good = new Good();
//        good.setGoodId("20191221024");
//        good.setOriginPrice(new BigDecimal(80000));
//        good.setDiscountPrice(new BigDecimal(4800));
//        good.setStock(5000L);
//        good.setStartTime(LocalDateTime.parse("2021-05-28T00:00:00"));
//        good.setEndTime(LocalDateTime.parse("2021-05-31T23:59:59"));
//
//        int insert = goodMapper.insert(good);
//        log.info("查询记录");
//        List<Good> goods = goodMapper.selectList(null);
//        goods.forEach(System.out::println);
//        log.info("修改记录");
//        good = goods.get(0);
//        good.setStock(200L);
//        int update = goodMapper.updateById(good);
//        goods = goodMapper.selectList(null);
//        goods.forEach(System.out::println);
//        log.info("删除记录");
//        goodMapper.deleteById(good);
//        goods = goodMapper.selectList(null);
//        goods.forEach(System.out::println);
//    }
//}
