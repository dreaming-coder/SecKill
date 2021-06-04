package com.example.rabbit.listener;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.repository.entity.Order;
import com.example.repository.mapper.OrderMapper;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author ice
 * @blog https://blog.csdn.net/dreaming_coder
 * @description
 * @create 2021-05-31 19:49:00
 */

@Component
@Slf4j
@RabbitListener(queues = "seckill.order.pay.queue")
public class OrderPayListener {

    private OrderMapper orderMapper;

    @Autowired
    public void setOrderMapper(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @RabbitHandler
    public void consumer(Order order, Message message, Channel channel) {
        // 根据传入的 order 中的 orderId 定位到确定的订单，将其状态修改为 0
        try {
            log.info("付款订单id：{}", order.getOrderId());
            UpdateWrapper<Order> wrapper = new UpdateWrapper<>();
            wrapper.eq("order_id", order.getOrderId()).eq("status", "1");
            Order update = new Order();
            update.setStatus("0");
            int affectedRows = orderMapper.update(update, wrapper);
            if (affectedRows > 0) {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            } else {
                throw new RuntimeException();
            }
        } catch (IOException e) {
            try {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } catch (RuntimeException runtimeException) {
            log.error("付款失败");
            try {
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
