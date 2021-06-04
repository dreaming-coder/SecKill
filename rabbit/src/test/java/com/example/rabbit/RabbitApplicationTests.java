//package com.example.rabbit;
//
//import com.example.repository.entity.Order;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//@SpringBootTest
//class RabbitApplicationTests {
//    @Autowired
//    private RabbitTemplate rabbitTemplate;
//
//    @Test
//    @DisplayName("测试 seckill.order.pay")
//    void text1() {
//        Order order = new Order();
//        order.setOrderId("11cbeb57989f628c2d0996812f5398a6");
//        rabbitTemplate.convertAndSend("order-event-exchange", "seckill.order.pay", order);
//    }
//
//    @Test
//    @DisplayName("测试 seckill.order.cancel")
//    void text2() {
//        Order order = new Order();
//        order.setOrderId("11cbeb57989f628c2d0996812f5398a6");
//        rabbitTemplate.convertAndSend("order-event-exchange", "seckill.order.cancel", order);
//    }
//
//}
