package com.example.rabbit.listener;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.redis.service.StockService;
import com.example.repository.entity.Good;
import com.example.repository.entity.Order;
import com.example.repository.mapper.GoodMapper;
import com.example.repository.mapper.OrderMapper;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author ice
 * @blog https://blog.csdn.net/dreaming_coder
 * @description
 * @create 2021-05-31 19:51:18
 */
@Component
@Slf4j
@RabbitListener(queues = "seckill.order.process.queue")
public class OrderProcessListener {

    private GoodMapper goodMapper;

    private OrderMapper orderMapper;

    private StockService stockService;

    private RabbitTemplate rabbitTemplate;

    @Autowired
    public void setRabbitTemplate(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Autowired
    public void setGoodMapper(GoodMapper goodMapper) {
        this.goodMapper = goodMapper;
    }

    @Autowired
    public void setStockService(StockService stockService) {
        this.stockService = stockService;
    }

    @Autowired
    public void setOrderMapper(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @RabbitHandler
    public void consumer(Order order, Message message, Channel channel) {
        try {
            QueryWrapper<Order> orderQueryWrapper = new QueryWrapper<>();
            orderQueryWrapper.eq("phone", order.getPhone()).eq("good_id", order.getGoodId())
                    .orderByDesc("create_time").last("limit 1");
            Order order_new = orderMapper.selectOne(orderQueryWrapper);

            if ("0".equals(order_new.getStatus())) {
                UpdateWrapper<Good> wrapper = new UpdateWrapper<>();
                wrapper.eq("good_id", order_new.getGoodId()).gt("stock", 0);
                int affectedRows = goodMapper.deliver(wrapper);
                if (affectedRows > 0) {
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                } else {
                    throw new RuntimeException();
                }
            } else {
                stockService.incrementStock(order.getGoodId());
                if ("1".equals(order_new.getStatus())) {
                    rabbitTemplate.convertAndSend("order-event-exchange", "seckill.order.cancel", order);
                }
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            }
        } catch (IOException e) {
            try {
                stockService.incrementStock(order.getGoodId());
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } catch (RuntimeException runtimeException) {
            runtimeException.printStackTrace();
            try {
                log.error("商品出库失败");
                stockService.incrementStock(order.getGoodId());
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
