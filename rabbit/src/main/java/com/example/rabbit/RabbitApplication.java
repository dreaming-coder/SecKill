package com.example.rabbit;

import com.example.repository.entity.Order;
import com.example.repository.mapper.OrderMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;

@SpringBootApplication(scanBasePackages = "com.example")
public class RabbitApplication {


    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(RabbitApplication.class, args);
        RabbitTemplate rabbitTemplate = run.getBean(RabbitTemplate.class);
        OrderMapper orderMapper = run.getBean(OrderMapper.class);
        List<Order> orders = orderMapper.selectList(null);
        orders.forEach(
                order -> rabbitTemplate.convertAndSend("order-event-exchange", "seckill.order.create", order)
        );

    }

}
