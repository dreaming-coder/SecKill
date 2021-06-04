//package com.example.service;
//
//import com.example.repository.entity.Good;
//import com.example.service.type.ReturnType;
//import com.example.service.pojo.OrderInfoDTO;
//import com.example.service.service.GoodService;
//import com.example.service.service.OrderService;
//import com.example.service.service.PayService;
//import com.example.service.service.SecKillService;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import java.util.List;
//
//@SpringBootTest
//class ServiceApplicationTests {
//
//    private GoodService goodService;
//
//    private OrderService orderService;
//
//    private PayService payService;
//
//    private SecKillService secKillService;
//
//    @Autowired
//    public void setGoodService(GoodService goodService) {
//        this.goodService = goodService;
//    }
//
//    @Autowired
//    public void setOrderService(OrderService orderService) {
//        this.orderService = orderService;
//    }
//
//    @Autowired
//    public void setPayService(PayService payService) {
//        this.payService = payService;
//    }
//
//    @Autowired
//    public void setSecKillService(SecKillService secKillService) {
//        this.secKillService = secKillService;
//    }
//
//    @Test
//    @DisplayName("测试 GoodService.goodList()")
//    void test1() {
//        List<Good> goods = goodService.goodList();
//        goods.forEach(System.out::println);
//    }
//
//    @Test
//    @DisplayName("测试 GoodService.getStock()")
//    void test2() {
//        Long stock = goodService.getStock("596935345022");
//        System.out.println(stock);
//    }
//
//    @Test
//    @DisplayName("测试 SecKillService.seckill()")
//    void test3(){
//        ReturnType returnType = secKillService.seckill("596935345022", "17512511216", "644476114@qq.com");
//        System.out.println(returnType);
//    }
//
//    @Test
//    @DisplayName("测试 OrderService.queryOrderInfo()")
//    void test4(){
//        OrderInfoDTO info = orderService.queryOrderInfo("596935345022", "17512511216");
//        System.out.println(info);
//    }
//
//    @Test
//    @DisplayName("测试 OrderService.pay()")
//    void test5(){
//        payService.pay("596935345022","17512511216");
//    }
//
//    @Test
//    @DisplayName("测试 OrderService.cancel()")
//    void test6(){
//        payService.cancel("596935345022","17512511216");
//    }
//
//    @Test
//    @DisplayName("测试 OrderService.later()")
//    void test7(){
//        payService.later("596935345022","17512511216");
//    }
//}
