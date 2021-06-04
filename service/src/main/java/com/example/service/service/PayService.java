package com.example.service.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.repository.entity.Order;
import com.example.repository.mapper.OrderMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author ice
 * @blog https://blog.csdn.net/dreaming_coder
 * @description
 * @create 2021-06-01 20:55:32
 */
@Service
public class PayService {
    private RabbitTemplate rabbitTemplate;
    private OrderMapper orderMapper;

    @Autowired
    public void setRabbitTemplate(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Autowired
    public void setOrderMapper(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    public void pay(String goodId, String phone) {
        QueryWrapper<Order> orderQueryWrapper = new QueryWrapper<>();
        orderQueryWrapper.eq("phone", phone).eq("good_id", goodId).eq("status", "1");
        Order order = orderMapper.selectOne(orderQueryWrapper);
        rabbitTemplate.convertAndSend("order-event-exchange", "seckill.order.pay", order);
    }

    public void later(String goodId, String phone) {

    }

    public void cancel(String goodId, String phone) {
        QueryWrapper<Order> orderQueryWrapper = new QueryWrapper<>();
        orderQueryWrapper.eq("phone", phone).eq("good_id", goodId).eq("status", "1");
        Order order = orderMapper.selectOne(orderQueryWrapper);
        rabbitTemplate.convertAndSend("order-event-exchange", "seckill.order.cancel", order);
    }
}
