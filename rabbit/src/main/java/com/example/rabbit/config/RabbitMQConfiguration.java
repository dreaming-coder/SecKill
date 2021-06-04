package com.example.rabbit.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ice
 * @blog https://blog.csdn.net/dreaming_coder
 * @description
 * @create 2021-05-30 19:52:59
 */
@Configuration
public class RabbitMQConfiguration {
    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // 定义交换机

    @Bean
    public Exchange orderEventExchange() {
        return new TopicExchange("order-event-exchange", true, false);
    }

    // 死信队列

    @Bean
    public Queue seckillOrderDelayQueue() {
        //死信队列的属性
        Map<String, Object> arguments = new HashMap<>();
        //指定order-event-exchange为死信交换机，消息过期后将会投到死信交换机
        arguments.put("x-dead-letter-exchange", "order-event-exchange");
        //指定死信的路由key
        arguments.put("x-dead-letter-routing-key", "seckill.order.process");
        //ttl time to live 消息存活时间，为了测试，设置为60s
        arguments.put("x-message-ttl", 60000);
        //队列名，是否持久，是否排他，是否自动删除
        return new Queue("seckill.order.delay.queue", true, false, false, arguments);
    }



    @Bean
    // 处理订单队列，根据订单表中订单状态决定库存释放还是减一入库
    public Queue seckillOrderProcessQueue() {
        return new Queue("seckill.order.process.queue", true, false, false);
    }

    @Bean
    // 支付队列，修改订单状态为 0
    public Queue seckillOrderPayQueue() {
        return new Queue("seckill.order.pay.queue", true, false, false);
    }

    @Bean
    // 支付队列，修改订单状态为 2
    public Queue seckillOrderCancelQueue() {
        return new Queue("seckill.order.cancel.queue", true, false, false);
    }


    // 绑定交换机和队列
    @Bean
    public Binding seckillOrderCreateBinding() {
        return new Binding("seckill.order.delay.queue", Binding.DestinationType.QUEUE,
                "order-event-exchange", "seckill.order.create", null);
    }

    @Bean
    public Binding seckillOrderProcessBinding() {
        return new Binding("seckill.order.process.queue", Binding.DestinationType.QUEUE,
                "order-event-exchange", "seckill.order.process", null);
    }
    @Bean
    public Binding seckillOrderPayBinding() {
        return new Binding("seckill.order.pay.queue", Binding.DestinationType.QUEUE,
                "order-event-exchange", "seckill.order.pay", null);
    }

    @Bean
    public Binding seckillOrderCancelBinding() {
        return new Binding("seckill.order.cancel.queue", Binding.DestinationType.QUEUE,
                "order-event-exchange", "seckill.order.cancel", null);
    }

}
