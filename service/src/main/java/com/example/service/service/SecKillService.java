package com.example.service.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.redis.service.LimitService;
import com.example.redis.service.StockService;
import com.example.repository.entity.Good;
import com.example.repository.entity.Order;
import com.example.repository.mapper.GoodMapper;
import com.example.repository.mapper.OrderMapper;
import com.example.service.type.ReturnType;
import com.google.common.util.concurrent.RateLimiter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author ice
 * @blog https://blog.csdn.net/dreaming_coder
 * @description
 * @create 2021-06-01 18:07:21
 */
@Service
@SuppressWarnings("UnstableApiUsage")
public class SecKillService {
    private RateLimiter rateLimiter;
    private LimitService limitService;
    private StockService stockService;
    private GoodMapper goodMapper;
    private OrderMapper orderMapper;
    private RabbitTemplate rabbitTemplate;

    @Autowired
    public void setRateLimiter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Autowired
    public void setLimitService(LimitService limitService) {
        this.limitService = limitService;
    }

    @Autowired
    public void setStockService(StockService stockService) {
        this.stockService = stockService;
    }

    @Autowired
    public void setGoodMapper(GoodMapper goodMapper) {
        this.goodMapper = goodMapper;
    }

    @Autowired
    public void setOrderMapper(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Autowired
    public void setRabbitTemplate(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public ReturnType seckill(String goodId, String phone, String email) {
        Boolean flag = limitService.checkRepeatLimit(goodId, phone, email);
        if (!flag) {
            return ReturnType.TimeLimitError;
        }
        Boolean isEnough = stockService.checkStock(goodId);
        if (!isEnough) {
            return ReturnType.StockOutError;
        }
        if (rateLimiter.tryAcquire()) {
            QueryWrapper<Good> wrapper = new QueryWrapper<>();
            wrapper.select("stock").eq("good_id", goodId);
            Long stock = goodMapper.selectOne(wrapper).getStock();
            if (stock == null || stock <= 0) {
                stockService.resetStock(goodId);
                return ReturnType.StockOutError;
            }
            // 判断该用户是否抢购过了
            QueryWrapper<Order> orderQueryWrapper = new QueryWrapper<>();
            orderQueryWrapper.eq("phone", phone).eq("good_id", goodId).ne("status", "2");
            List<Order> orders = orderMapper.selectList(orderQueryWrapper);
            if (orders.size() != 0) {
                stockService.incrementStock(goodId);
                return ReturnType.PurchaseLimitError;
            }

            // 一切就绪，开始生成订单，默认就是待支付状态，status = 1
            Order order = new Order();
            order.setPhone(phone);
            order.setEmail(email);
            order.setGoodId(goodId);
            order.setStatus("1");
            int insert = orderMapper.insert(order);
            if (insert > 0) {
                // 将该订单送入死信队列，等待处理
                rabbitTemplate.convertAndSend("order-event-exchange", "seckill.order.create", order);
            } else {
                stockService.incrementStock(goodId);
                return ReturnType.SeckillFailError;
            }
        } else {
            stockService.incrementStock(goodId);
            return ReturnType.SeckillFailError;
        }
        return ReturnType.Success;
    }
}
